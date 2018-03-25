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

import com.google.inject.Module
import com.twitter.inject.{Injector, TwitterModule}
import fr.cnrs.liris.accio.storage.Storage
import fr.cnrs.liris.accio.storage.elastic.ElasticStorageModule
import fr.cnrs.liris.accio.storage.memory.MemoryStorageModule

import scala.util.control.NonFatal

/**
 * Guice module provisioning storage-related services.
 */
object StorageModule extends TwitterModule {
  private[this] val typeFlag = flag("storage.type", "memory", "Storage type")

  override def modules: Seq[Module] =
    typeFlag.get match {
      case Some("memory") => Seq(MemoryStorageModule)
      case Some("es") => Seq(ElasticStorageModule)
      case Some(invalid) => throw new IllegalArgumentException(s"Invalid storage type: $invalid")
      case None =>
        // This only happen when this method is called the first time, during App's initialization.
        // We provide the entire list of all modules, making all flags available.
        Seq(MemoryStorageModule, ElasticStorageModule)
    }

  override def singletonStartup(injector: Injector): Unit = {
    injector.instance[Storage].startAsync()
  }

  override def singletonShutdown(injector: Injector): Unit = {
    try {
      injector.instance[Storage].stopAsync().awaitTerminated()
    } catch {
      case NonFatal(e) => logger.warn("Error while shutting down storage", e)
    }
  }
}