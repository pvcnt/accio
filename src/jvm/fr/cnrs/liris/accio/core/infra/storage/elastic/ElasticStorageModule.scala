/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.infra.storage.elastic

import com.google.inject.Provides
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.twitter.finatra.json.FinatraObjectMapper
import fr.cnrs.liris.accio.core.application.Configurable
import fr.cnrs.liris.accio.core.domain.RunRepository
import net.codingwell.scalaguice.ScalaModule
import org.elasticsearch.common.settings.Settings

/**
 *
 * @param clusterAddr
 * @param prefix
 * @param sniff
 */
case class ElasticStorageConfig(
  clusterAddr: String,
  prefix: String,
  sniff: Boolean = true)

/**
 * Guice module provisioning repositories with storage inside an Elasticsearch cluster.
 */
final class ElasticStorageModule extends ScalaModule with Configurable[ElasticStorageConfig] {
  override def configClass: Class[ElasticStorageConfig] = classOf[ElasticStorageConfig]

  override def configure(): Unit = {}

  @Provides
  def providesRunRepository(mapper: FinatraObjectMapper): RunRepository = {
    new ElasticRunRepository(mapper, client, config.prefix)
  }

  private lazy val client = {
    val settings = Settings.builder().put("client.transport.sniff", config.sniff).build()
    val uri = ElasticsearchClientUri(s"elasticsearch://${config.clusterAddr}")
    val client = ElasticClient.transport(settings, uri)

    sys.addShutdownHook(client.close())

    client
  }
}
