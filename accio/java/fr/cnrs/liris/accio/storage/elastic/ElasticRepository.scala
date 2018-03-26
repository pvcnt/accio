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

package fr.cnrs.liris.accio.storage.elastic

import com.google.common.util.concurrent.AbstractIdleService
import com.google.inject.Singleton
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers.KeywordAnalyzer
import com.sksamuel.elastic4s.get.RichGetResponse
import com.sksamuel.elastic4s.indexes.RichIndexResponse
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.sksamuel.elastic4s.searches.RichSearchResponse
import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl}
import com.twitter.bijection.Conversion.asMethod
import com.twitter.bijection.twitter_util.UtilBijections._
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.util.{Await, Future}
import com.twitter.inject.Logging
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.storage._
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.search.sort.SortOrder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.reflectiveCalls
import scala.util.control.NonFatal

private[elastic] abstract class ElasticRepository(client: ElasticClient)
  extends AbstractIdleService with Logging {
  
  protected def indexName: String

  protected def typeName: String

  override final protected def startUp(): Unit = {
    val f = client
      .execute(indexExists(indexName))
      .as[Future[IndicesExistsResponse]]
      .flatMap { resp =>
        if (!resp.isExists) {
          createMappings()
        } else {
          Future.value(true)
        }
      }
      .onFailure { case NonFatal(e) => logger.error(s"Failed to initialize $indexName/$typeName index", e) }
      .onSuccess { _ => logger.info(s"Created $indexName/$typeName index") }
    Await.ready(f)
  }

  protected def createMappings(): Future[CreateIndexResponse]

  override final protected def shutDown(): Unit = {}

  /**
   * Ensure all repositories are started.
   */
  protected final def ensureRunning(): Unit = synchronized {
    if (!isRunning) {
      startAsync().awaitRunning()
    }
  }
}
