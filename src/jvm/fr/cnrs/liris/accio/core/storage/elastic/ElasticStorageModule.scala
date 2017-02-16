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

package fr.cnrs.liris.accio.core.storage.elastic

import com.google.inject.{Provides, Singleton}
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.twitter.finatra.json.FinatraObjectMapper
import fr.cnrs.liris.accio.core.storage.{ForStorage, MutableRunRepository, MutableTaskRepository, MutableWorkflowRepository}
import net.codingwell.scalaguice.ScalaModule
import org.elasticsearch.common.settings.Settings

/**
 * Guice module provisioning Elasticsearch storage.
 *
 * @param config Configuration.
 */
final class ElasticStorageModule(config: ElasticStorageConfig) extends ScalaModule {
  override def configure(): Unit = {
    bind[StorageConfig].toInstance(config.toConfig)
    bind[MutableRunRepository].to[ElasticRunRepository]
    bind[MutableTaskRepository].to[ElasticTaskRepository]
    bind[MutableWorkflowRepository].to[ElasticWorkflowRepository]
  }

  @Provides
  @Singleton
  @ForStorage
  def providesObjectMapper(mapperFactory: ObjectMapperFactory): FinatraObjectMapper = mapperFactory.create()

  @Provides
  @Singleton
  @ForStorage
  def providesElasticClient: ElasticClient = {
    val settings = Settings.builder()
      //.put("client.transport.sniff", true)
      .put("cluster.name", "elasticsearch")
      .build()
    val uri = ElasticsearchClientUri(s"elasticsearch://${config.addr}")
    ElasticClient.transport(settings, uri)
  }
}