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

package fr.cnrs.liris.lumos.storage.install

import com.google.inject.{Inject, Provider, Singleton}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.Await
import fr.cnrs.liris.lumos.storage.JobStore
import fr.cnrs.liris.lumos.storage.memory.MemoryJobStore
import fr.cnrs.liris.lumos.storage.mysql.{MysqlClientFactory, MysqlJobStore}

/**
 * Guice module provisioning storage.
 */
object StorageModule extends TwitterModule {
  private[this] val typeFlag = flag("storage", "memory", "Storage type")

  // MySQL storage configuration.
  private[this] val myServerFlag = flag("storage.mysql.server", "localhost:3306", "MySQL server address")
  private[this] val myUserFlag = flag("storage.mysql.user", "root", "MySQL username")
  private[this] val myPasswordFlag = flag[String]("storage.mysql.password", "MySQL password")
  private[this] val myDatabaseFlag = flag[String]("storage.mysql.database", "lumos", "MySQL database name")

  override def configure(): Unit = {
    typeFlag() match {
      case "memory" => bind[JobStore].toProvider[MemoryJobStoreProvider].in[Singleton]
      case "mysql" => bind[JobStore].toProvider[MysqlJobStoreProvider].in[Singleton]
      case unknown => throw new IllegalArgumentException(s"Unknown storage type: $unknown")
    }
  }

  override def singletonStartup(injector: Injector): Unit = {
    Await.ready(injector.instance[JobStore].startUp())
  }

  override def singletonShutdown(injector: Injector): Unit = {
    Await.ready(injector.instance[JobStore].close())
  }

  private class MemoryJobStoreProvider @Inject()(statsReceiver: StatsReceiver)
    extends Provider[JobStore] {

    override def get(): JobStore = new MemoryJobStore(statsReceiver)
  }

  private class MysqlJobStoreProvider @Inject()(statsReceiver: StatsReceiver)
    extends Provider[JobStore] {

    override def get(): JobStore = {
      val client = MysqlClientFactory(myServerFlag(), myUserFlag(), myPasswordFlag.get.orNull, myDatabaseFlag())
      new MysqlJobStore(client, statsReceiver)
    }
  }

}