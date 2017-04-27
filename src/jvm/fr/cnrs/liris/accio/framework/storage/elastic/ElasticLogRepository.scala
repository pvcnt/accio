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

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers.KeywordAnalyzer
import com.sksamuel.elastic4s.bulk.RichBulkResponse
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.sksamuel.elastic4s.searches.RichSearchResponse
import com.twitter.bijection.Conversion.asMethod
import com.twitter.bijection.twitter_util.UtilBijections._
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.util.{Await, Duration, Future}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.framework.api.thrift._
import fr.cnrs.liris.accio.framework.storage._
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.reindex.BulkIndexByScrollResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.reflectiveCalls
import scala.util.control.NonFatal

/**
 * Log repository persisting data into Elasticsearch.
 *
 * @param mapper  Finatra object mapper.
 * @param client  Elasticsearch client.
 * @param prefix  Prefix of indexes.
 * @param timeout Query timeout.
 */
@Singleton
private[elastic] final class ElasticLogRepository @Inject()(
  mapper: FinatraObjectMapper,
  client: ElasticClient,
  @ElasticPrefix prefix: String,
  @ElasticTimeout timeout: Duration)
  extends ElasticRepository(client) with MutableLogRepository with StrictLogging {

  override def find(query: LogsQuery): Seq[RunLog] = {
    var q = boolQuery()
      .filter(termQuery("run_id.value", query.runId.value))
      .filter(termQuery("node_name", query.nodeName))
    query.classifier.foreach { classifier =>
      q = q.filter(termQuery("classifier", classifier))
    }
    query.since.foreach { since =>
      q = q.filter(rangeQuery("created_at").from(since.inMillis).includeLower(false))
    }
    val s = search(indexName / typeName)
      .query(q)
      .sortBy(fieldSort("created_at"))
      // https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-from-size.html
      .limit(query.limit.getOrElse(10000))

    val f = client
      .execute(s)
      .as[Future[RichSearchResponse]]
      .map { resp =>
        resp.hits.toSeq.map(hit => mapper.parse[RunLog](hit.sourceAsBytes))
      }
      .rescue {
        case _: IndexNotFoundException => Future.value(Seq.empty)
        case NonFatal(e) =>
          logger.error("Error while searching logs", e)
          Future.value(Seq.empty)
      }
    Await.result(f, timeout)
  }

  override def save(logs: Seq[RunLog]): Unit = {
    val actions = logs.map { log =>
      val json = mapper.writeValueAsString(log)
      indexInto(indexName / typeName).source(json)
    }
    val f = client
      .execute(bulk(actions))
      .as[Future[RichBulkResponse]]
      .onFailure(e => logger.error(s"Error while saving ${logs.size} logs", e))
      .unit
    Await.result(f, timeout)
  }

  override def remove(id: RunId): Unit = {
    val f = client
      .execute(deleteIn(indexName).by(termQuery("id.value", id.value)))
      .as[Future[BulkIndexByScrollResponse]]
      .unit
    Await.result(f, timeout)
  }

  override protected def indexName = s"${prefix}logs"

  override protected def typeName = "default"

  override protected def createMappings(): Future[CreateIndexResponse] = {
    // Some fields must absolutely be indexed with the keyword analyzer, which performs no tokenization at all,
    // otherwise they won't be searchable by their exact value (which can be annoying, e.g., for ids).
    val fields = Seq(
      objectField("run_id").as(textField("value").analyzer(KeywordAnalyzer)),
      textField("node_name").analyzer(KeywordAnalyzer),
      longField("created_at"),
      textField("classifier").analyzer(KeywordAnalyzer))
    client
      .execute(createIndex(indexName).mappings(new MappingDefinition(typeName) as (fields: _*)))
      .as[Future[CreateIndexResponse]]
  }
}
