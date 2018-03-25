/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.storage.elastic

import com.google.inject.{Provides, Singleton}
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.{Injector, TwitterPrivateModule}
import com.twitter.util.Duration
import fr.cnrs.liris.accio.storage.Storage
import org.elasticsearch.common.settings.Settings

/**
 * Guice module provisioning Elasticsearch storage.
 */
object ElasticStorageModule extends TwitterPrivateModule {
  private[this] val addrFlag = flag("storage.es.addr", "127.0.0.1:9300", "Address to Elasticsearch cluster")
  private[this] val prefixFlag = flag("storage.es.prefix", "accio_", "Prefix of Elasticsearch indices")
  private[this] val timeoutFlag = flag("storage.es.timeout", Duration.Top, "Timeout when querying Elasticsearch")

  override def configure(): Unit = {
    bind[String].annotatedWith[ElasticPrefix].toInstance(prefixFlag())
    bind[Duration].annotatedWith[ElasticTimeout].toInstance(timeoutFlag())
    bind[Storage].to[ElasticStorage].asEagerSingleton()
    expose[Storage]
  }

  @Provides @Singleton
  def providesObjectMapper(mapperFactory: ObjectMapperFactory): FinatraObjectMapper = mapperFactory.create()

  @Provides @Singleton
  def providesElasticClient: ElasticClient = {
    val settings = Settings.builder()
      //.put("client.transport.sniff", true)
      .put("cluster.name", "elasticsearch")
      .build()
    val uri = ElasticsearchClientUri(s"elasticsearch://${addrFlag()}")
    ElasticClient.transport(settings, uri)
  }

  override def singletonShutdown(injector: Injector): Unit = {
    injector.instance[ElasticClient].close()
  }
}