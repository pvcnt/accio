/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Accio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Accio.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnrs.liris.accio.storage.mysql

import java.util.concurrent.locks.ReentrantLock

import com.twitter.finagle.mysql.Client
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.{Await, Future}
import fr.cnrs.liris.accio.storage.{RunStore, Storage, StoreProvider, WorkflowStore}

private[storage] final class MysqlStorage(
  client: Client,
  statsReceiver: StatsReceiver,
  useNativeLocks: Boolean)
  extends Storage {

  private[this] val workflowStore = new MysqlWorkflowStore(client, statsReceiver)
  private[this] val runStore = new MysqlRunStore(client, statsReceiver)
  private[this] val storeProvider = new StoreProvider.Mutable {
    override def runs: RunStore.Mutable = runStore

    override def workflows: WorkflowStore.Mutable = workflowStore
  }
  private[this] val writeWaitStat = statsReceiver.stat("storage", "mysql", "write_wait_nanos")
  private[this] val writeLock = if (useNativeLocks) new MysqlLock(client, "accio_write_lock") else new ReentrantLock

  override def read[T](fn: StoreProvider => T): T = fn(storeProvider)

  override def write[T](fn: StoreProvider.Mutable => T): T = {
    val start = System.nanoTime()
    writeLock.lock()
    try {
      writeWaitStat.add(System.nanoTime() - start)
      fn(storeProvider)
    } finally {
      writeLock.unlock()
    }
  }

  override def startUp(): Unit = {
    val fs = MysqlStorage.Ddl.map(ddl => client.query(ddl).unit)
    Await.result(Future.join(fs))
  }

  override def shutDown(): Unit = Await.result(client.close())
}

object MysqlStorage {
  private val Ddl = Seq(
    "create table if not exists runs(" +
      "unused_id int not null auto_increment," +
      "id varchar(255) not null," +
      // Longblob is up to 4GB. We do not need so much space, but mediumblob is only up to 16Mb,
      // which is not sufficient for runs with large results. The `max_allowed_packet` parameter
      // of MySQL has to be set accordingly (but weirdly its maximum value is "only" 1GB).
      "content longblob not null," +
      "primary key (unused_id)," +
      "UNIQUE KEY uix_id(id)" +
      ") ENGINE=InnoDB DEFAULT CHARSET=utf8",

    "create table if not exists workflows(" +
      "unused_id int not null auto_increment," +
      "id varchar(255) not null," +
      "version varchar(255) not null," +
      "is_active tinyint(1) not null," +
      "content mediumblob not null," + // Mediumblob is up to 16MB
      "primary key (unused_id)," +
      "UNIQUE KEY uix_id_version(id, version)" +
      ") ENGINE=InnoDB DEFAULT CHARSET=utf8")
}