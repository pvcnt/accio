/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.storage.inject

import java.nio.file.Paths

import com.twitter.inject.TwitterModule
import com.twitter.util.Duration
import fr.cnrs.liris.accio.core.storage._
import fr.cnrs.liris.accio.core.storage.elastic.{ElasticStorageConfig, ElasticStorageModule}
import fr.cnrs.liris.accio.core.storage.local.{LocalStorageConfig, LocalStorageModule}
import fr.cnrs.liris.accio.core.storage.memory.MemoryStorageModule

/**
 * Guice module provisioning storage-related services.
 */
object StorageModule extends TwitterModule {
  private[this] val storageFlag = flag("storage.type", "memory", "Storage type")

  // Local storage configuration.
  private[this] val localStoragePathFlag = flag[String]("storage.local.path", "Path where to store data")

  // Elasticsearch storage configuration.
  private[this] val esStorageAddrFlag = flag("storage.es.addr", "127.0.0.1:9300", "Address to Elasticsearch cluster")
  private[this] val esStoragePrefixFlag = flag("storage.es.prefix", "accio_", "Prefix of Elasticsearch indices")
  private[this] val esStorageQueryTimeoutFlag = flag("storage.es.query_timeout", Duration.Top, "Elasticsearch query timeout")

  protected override def configure(): Unit = {
    val module = storageFlag() match {
      case "memory" => new MemoryStorageModule
      case "local" => new LocalStorageModule(LocalStorageConfig(Paths.get(localStoragePathFlag())))
      case "es" =>
        val config = ElasticStorageConfig(esStorageAddrFlag(), esStoragePrefixFlag(), esStorageQueryTimeoutFlag())
        new ElasticStorageModule(config)
      case unknown => throw new IllegalArgumentException(s"Unknown storage type: $unknown")
    }
    install(module)

    // Bind read-only repositories to mutable repositories.
    bind[RunRepository].to[MutableRunRepository]
    bind[WorkflowRepository].to[MutableWorkflowRepository]
    bind[TaskRepository].to[MutableTaskRepository]
  }
}