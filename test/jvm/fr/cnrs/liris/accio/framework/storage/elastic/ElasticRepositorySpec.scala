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

package fr.cnrs.liris.accio.framework.storage.elastic

import java.nio.file.Files

import com.sksamuel.elastic4s.embedded.LocalNode
import com.sksamuel.elastic4s.testkit.{AbstractElasticSugar, ClassLocalNodeProvider, LocalNodeProvider}
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.index.reindex.ReindexPlugin
import org.elasticsearch.painless.PainlessPlugin
import org.elasticsearch.percolator.PercolatorPlugin
import org.elasticsearch.script.mustache.MustachePlugin
import org.elasticsearch.transport.Netty4Plugin
import org.scalatest.{BeforeAndAfterAll, Suite}

/**
 * Trait for unit tests that need a local Elasticsearch node to be launched. It is basically a copy/paste from
 * [[com.sksamuel.elastic4s.testkit.ElasticSugar]], except that we include the [[ReindexPlugin]] that is
 * desperately needed for the delete-by-query and update-by-query actions.
 */
private[elastic] trait ElasticRepositorySpec extends AbstractElasticSugar with LocalNodeProvider with BeforeAndAfterAll {
  this: Suite =>

  final override def getNode: LocalNode = {
    val pathHome = Files.createTempDirectory(getClass.getSimpleName)
    val settings = Settings.builder
      .put("cluster.name", "node_" + ClassLocalNodeProvider.counter.getAndIncrement())
      .put("path.home", pathHome.toAbsolutePath.toString)
      .put("path.repo", pathHome.resolve("repo").toAbsolutePath.toString)
      .put("path.data", pathHome.resolve("data").toAbsolutePath.toString)
      .put("transport.type", "local")
      .put("http.type", "netty4")
      .build
    val plugins = List(classOf[Netty4Plugin], classOf[MustachePlugin], classOf[PercolatorPlugin], classOf[ReindexPlugin], classOf[PainlessPlugin])
    new LocalNode(settings, plugins)
  }

  override def afterAll(): Unit = {
    node.stop(true)
  }
}