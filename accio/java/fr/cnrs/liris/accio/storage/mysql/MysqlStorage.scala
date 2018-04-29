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

import com.twitter.finagle.mysql.Client
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.{Await, Future}
import fr.cnrs.liris.accio.storage.{JobStore, Storage}

private[storage] final class MysqlStorage(client: Client, statsReceiver: StatsReceiver)
  extends Storage {

  override val jobs: JobStore = new MysqlJobStore(client, statsReceiver)

  override def startUp(): Unit = {
    val fs = MysqlStorage.Ddl.map(ddl => client.query(ddl).unit)
    Await.result(Future.join(fs))
  }

  override def shutDown(): Unit = Await.result(client.close())
}

object MysqlStorage {
  private val Ddl = Seq(
    "create table if not exists jobs(" +
      "unused_id int not null auto_increment," +
      "name varchar(255) not null," +
      "parent varchar(255) null," +
      // Longblob is up to 4GB. We do not need so much space, but mediumblob is only up to 16Mb,
      // which is not sufficient for runs with large results. The `max_allowed_packet` parameter
      // of MySQL has to be set accordingly (but weirdly its maximum value is "only" 1GB).
      "content longblob not null," +
      "primary key (unused_id)," +
      "UNIQUE KEY uix_name(name)" +
      ") ENGINE=InnoDB DEFAULT CHARSET=utf8")
}