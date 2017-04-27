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
import com.twitter.util.{Await, Duration, Future}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.framework.api.thrift._
import fr.cnrs.liris.accio.framework.storage._
import fr.cnrs.liris.accio.framework.util.Lockable
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.search.sort.SortOrder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.reflectiveCalls
import scala.util.control.NonFatal

/**
 * Run repository persisting data into Elasticsearch.
 *
 * @param mapper  Finatra object mapper.
 * @param client  Elasticsearch client.
 * @param prefix  Prefix of indexes.
 * @param timeout Query timeout.
 */
@Singleton
private[elastic] final class ElasticRunRepository @Inject() (
  mapper: FinatraObjectMapper,
  client: ElasticClient,
  @ElasticPrefix prefix: String,
  @ElasticTimeout timeout: Duration)
  extends ElasticRepository(client) with MutableRunRepository with Lockable[String] with StrictLogging {

  override def find(query: RunQuery): RunList = {
    ensureRunning()
    var q = boolQuery()
    query.owner.foreach { owner =>
      q = q.must(matchQuery("owner.name", owner))
    }
    if (query.status.nonEmpty) {
      q = q.filter(termsQuery("state.status", query.status.map(_.value)))
    }
    query.workflow.foreach { workflowId =>
      q = q.filter(termQuery("pkg.workflow_id.value", workflowId.value))
    }
    query.name.foreach { name =>
      q = q.must(matchQuery("name", name))
    }
    query.clonedFrom.foreach { clonedFrom =>
      q = q.filter(termQuery("cloned_from.value", clonedFrom.value))
    }
    query.tags.foreach { tag =>
      q = q.filter(termQuery("tags", tag))
    }
    query.parent match {
      case Some(parent) => q = q.filter(termQuery("parent.value", parent.value))
      case None => q = q.filter(not(existsQuery("parent")))
    }
    query.q.foreach { qs =>
      q = q
        .should(matchQuery("owner.name", qs))
        .should(matchQuery("name", qs))
        .should(termQuery("pkg.workflow_id.value", qs))
        //TODO: wrong, we should not tokenize by hand here with split().
        .should(qs.split(" ").map(s => termQuery("tags", s)))
        .minimumShouldMatch(1)
    }

    var s = search(indexName / typeName)
      .query(q)
      .sourceExclude("state.nodes.result")
      .limit(query.limit.getOrElse(10000)) // Max limit defaults to 10000.
      .from(query.offset.getOrElse(0))
    if (query.q.isEmpty) {
      s = s.sortBy(fieldSort("created_at").order(SortOrder.DESC))
    }

    val f = client
      .execute(s)
      .as[Future[RichSearchResponse]]
      .map { resp =>
        val results = resp.hits.toSeq.map(hit => mapper.parse[Run](hit.sourceAsBytes))
        RunList(results, resp.totalHits.toInt)
      }
      .rescue {
        case _: IndexNotFoundException => Future.value(RunList(Seq.empty, 0))
        case NonFatal(e) =>
          logger.error("Error while searching runs", e)
          Future.value(RunList(Seq.empty, 0))
      }
    Await.result(f, timeout)
  }

  override def get(id: RunId): Option[Run] = {
    ensureRunning()
    val f = client
      .execute(ElasticDsl.get(id.value).from(indexName / typeName))
      .as[Future[RichGetResponse]]
      .map { resp =>
        if (resp.isSourceEmpty) {
          None
        } else {
          Some(mapper.parse[Run](resp.sourceAsString))
        }
      }
      .rescue {
        case _: IndexNotFoundException => Future.value(None)
        case NonFatal(e) =>
          logger.error(s"Error while retrieving run ${id.value}", e)
          Future.value(None)
      }
    Await.result(f, timeout)
  }

  override def save(run: Run): Unit = locked(run.id.value) {
    ensureRunning()
    val json = mapper.writeValueAsString(run)
    val f = client
      .execute(indexInto(indexName / typeName).id(run.id.value).source(json))
      .as[Future[RichIndexResponse]]
      .onFailure(e => logger.error(s"Error while saving run ${run.id.value}", e))
      .unit
    Await.result(f, timeout)
  }

  override def remove(id: RunId): Unit = locked(id.value) {
    ensureRunning()
    val f = client
      .execute(delete(id.value).from(indexName / typeName))
      .as[Future[DeleteResponse]]
      .unit
    Await.result(f, timeout)
  }

  override def transactional[T](id: RunId)(fn: Option[Run] => T): T = locked(id.value) {
    fn(get(id))
  }

  override def get(cacheKey: CacheKey): Option[OpResult] = {
    ensureRunning()
    val q = nestedQuery("state.nodes")
      .query(termQuery("state.nodes.cache_key.hash", cacheKey.hash))
      .scoreMode(ScoreMode.None)
    val s = search(indexName / typeName).query(q).size(1)
    val f = client
      .execute(s)
      .as[Future[RichSearchResponse]]
      .map { resp =>
        if (resp.totalHits > 0) {
          val run = mapper.parse[Run](resp.hits.head.sourceAsString)
          run.state.nodes.find(_.cacheKey.contains(cacheKey)).get.result
        } else {
          None
        }
      }
      .rescue {
        case _: IndexNotFoundException => Future.value(None)
        case NonFatal(e) =>
          logger.error(s"Error while retrieving cached result ${cacheKey.hash}", e)
          Future.value(None)
      }
    Await.result(f, timeout)
  }

  override protected def indexName = s"${prefix}runs"

  override protected def typeName = "default"

  override protected def createMappings(): Future[CreateIndexResponse] = {
    // Some fields must absolutely be indexed with the keyword analyzer, which performs no tokenization at all,
    // otherwise they won't be searchable by their exact value (which can be annoying, e.g., for ids).
    val fields = Seq(
      objectField("id").as(textField("value").analyzer(KeywordAnalyzer)),
      objectField("pkg").as(
        objectField("workflow_id").as(textField("value").analyzer(KeywordAnalyzer)),
        textField("version").analyzer(KeywordAnalyzer)
      ),
      objectField("parent").as(textField("value").analyzer(KeywordAnalyzer)),
      objectField("cloned_from").as(textField("value").analyzer(KeywordAnalyzer)),
      longField("created_at"),
      objectField("params").enabled(false),
      objectField("state").as(
        nestedField("nodes").as(
          textField("name").analyzer(KeywordAnalyzer),
          objectField("cache_key").as(textField("hash").analyzer(KeywordAnalyzer)),
          objectField("result").enabled(false)
        )
      ))
    client
      .execute(createIndex(indexName).mappings(new MappingDefinition(typeName) as (fields: _*)))
      .as[Future[CreateIndexResponse]]
  }
}
