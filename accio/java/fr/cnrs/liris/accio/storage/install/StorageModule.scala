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

package fr.cnrs.liris.accio.storage.install

import com.google.inject.{Inject, Provider, Singleton}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.{Injector, TwitterModule}
import fr.cnrs.liris.accio.storage.Storage
import fr.cnrs.liris.accio.storage.memory.MemoryStorage
import fr.cnrs.liris.accio.storage.mysql.{MysqlClientFactory, MysqlStorage}

/**
 * Guice module provisioning storage.
 */
object StorageModule extends TwitterModule {
  private[this] val typeFlag = flag("storage", "memory", "Storage type")

  // MySQL storage configuration.
  private[this] val myServerFlag = flag("storage.mysql_server", "localhost:3306", "MySQL server address")
  private[this] val myUserFlag = flag("storage.mysql_user", "root", "MySQL username")
  private[this] val myPasswordFlag = flag[String]("storage.mysql_password", "MySQL password")
  private[this] val myDatabaseFlag = flag[String]("storage.mysql_database", "accio", "MySQL database name")

  override def configure(): Unit = {
    typeFlag() match {
      case "memory" => bind[Storage].toProvider[MemoryStorageProvider].in[Singleton]
      case "mysql" => bind[Storage].toProvider[MysqlStorageProvider].in[Singleton]
      case unknown => throw new IllegalArgumentException(s"Unknown storage type: $unknown")
    }
  }

  override def singletonStartup(injector: Injector): Unit = {
    injector.instance[Storage].startUp()
  }

  override def singletonShutdown(injector: Injector): Unit = {
    injector.instance[Storage].shutDown()
  }

  private class MemoryStorageProvider @Inject()(statsReceiver: StatsReceiver)
    extends Provider[Storage] {

    override def get(): Storage = new MemoryStorage(statsReceiver)
  }

  private class MysqlStorageProvider @Inject()(statsReceiver: StatsReceiver)
    extends Provider[Storage] {

    override def get(): Storage = {
      val client = MysqlClientFactory(myServerFlag(), myUserFlag(), myPasswordFlag.get.orNull, myDatabaseFlag())
      new MysqlStorage(client, statsReceiver)
    }
  }

}