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

package fr.cnrs.liris.accio.storage.inject

import com.twitter.inject.{Injector, TwitterModule}
import fr.cnrs.liris.accio.storage.Storage
import fr.cnrs.liris.accio.storage.memory.MemoryStorage

/**
 * Guice module provisioning storage-related services.
 */
object StorageModule extends TwitterModule {
  private[this] val typeFlag = flag("storage.type", "memory", "Storage type")

  override def configure(): Unit = {
    typeFlag() match {
      case "memory" => bind[Storage].to[MemoryStorage].asEagerSingleton()
      case unknown => throw new IllegalArgumentException(s"Unknown storage type: $unknown")
    }
  }

  override def singletonStartup(injector: Injector): Unit = {
    injector.instance[Storage].startAsync().awaitRunning()
  }

  override def singletonShutdown(injector: Injector): Unit = {
    injector.instance[Storage].stopAsync().awaitTerminated()
  }
}