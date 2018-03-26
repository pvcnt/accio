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

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers.KeywordAnalyzer
import com.sksamuel.elastic4s.get.RichGetResponse
import com.sksamuel.elastic4s.indexes.RichIndexResponse
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.sksamuel.elastic4s.script.ScriptDefinition
import com.sksamuel.elastic4s.searches.RichSearchResponse
import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl}
import com.twitter.bijection.Conversion.asMethod
import com.twitter.bijection.twitter_util.UtilBijections._
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.util.{Await, Duration, Future}
import com.twitter.inject.Logging
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.storage.{MutableWorkflowRepository, WorkflowList, WorkflowQuery}
import fr.cnrs.liris.accio.util.Lockable
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.reindex.BulkIndexByScrollResponse
import org.elasticsearch.search.sort.SortOrder

import scala.concurrent.ExecutionContext.Implicits.global
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
private[elastic] final class ElasticWorkflowRepository @Inject()(
  mapper: FinatraObjectMapper,
  client: ElasticClient,
  @ElasticPrefix prefix: String,
  @ElasticTimeout timeout: Duration)
  extends ElasticRepository(client)
  with MutableWorkflowRepository with Lockable[String] with Logging {

  override def find(query: WorkflowQuery): WorkflowList = {
    ensureRunning()
    var q = boolQuery().filter(termQuery("is_active", 1))
    query.owner.foreach { owner =>
      q = q.must(matchQuery("owner.name", owner))
    }
    query.name.foreach { name =>
      q = q.must(matchQuery("name", name))
    }
    query.q.foreach { qs =>
      q = q
        .should(matchQuery("name", qs))
        .should(matchQuery("owner.name", qs))
        .minimumShouldMatch(1)
    }
    var s = search(indexName / typeName)
      .query(q)
      .sourceExclude("graph.nodes")
      .limit(query.limit.getOrElse(10000)) // Max limit defaults to 10000.
      .from(query.offset.getOrElse(0))
    if (query.q.isEmpty) {
      s = s.sortBy(fieldSort("created_at").order(SortOrder.DESC))
    }

    val f = client
      .execute(s)
      .as[Future[RichSearchResponse]]
      .map { resp =>
        val results = resp.hits.toSeq.map(hit => mapper.parse[Workflow](hit.sourceAsBytes))
        WorkflowList(results, resp.totalHits.toInt)
      }
      .rescue {
        case _: IndexNotFoundException => Future.value(WorkflowList(Seq.empty, 0))
        case NonFatal(e) =>
          logger.error("Error while searching workflows", e)
          Future.value(WorkflowList(Seq.empty, 0))
      }
    Await.result(f, timeout)
  }

  override def get(id: WorkflowId): Option[Workflow] = {
    ensureRunning()
    val q = boolQuery()
      .filter(termQuery("id.value", id.value))
      .filter(termQuery("is_active", 1))

    val f = client
      .execute(search(indexName / typeName).query(q).limit(1))
      .as[Future[RichSearchResponse]]
      .map { resp =>
        if (resp.totalHits > 0) {
          Some(mapper.parse[Workflow](resp.hits.head.sourceAsString))
        } else {
          None
        }
      }
      .rescue {
        case _: IndexNotFoundException => Future.value(None)
        case NonFatal(e) =>
          logger.error(s"Error while retrieving workflow ${id.value}", e)
          Future.value(None)
      }
    Await.result(f, timeout)
  }

  override def get(id: WorkflowId, version: String): Option[Workflow] = {
    ensureRunning()
    val f = client
      .execute(ElasticDsl.get(internalId(id, version)).from(indexName / typeName))
      .as[Future[RichGetResponse]]
      .map { resp =>
        if (resp.isSourceEmpty) {
          None
        } else {
          Some(mapper.parse[Workflow](resp.sourceAsString))
        }
      }
      .rescue {
        case _: IndexNotFoundException => Future.value(None)
        case NonFatal(e) =>
          logger.error(s"Error while retrieving workflow ${id.value}", e)
          Future.value(None)
      }
    Await.result(f, timeout)
  }

  override def save(workflow: Workflow): Unit = {
    ensureRunning()
    val json = mapper.writeValueAsString(workflow)
    val f = client
      .execute {
        // We insert the new version.
        indexInto(indexName / typeName)
          .id(internalId(workflow.id, workflow.version.get))
          .source(json)
          .refresh(RefreshPolicy.WAIT_UNTIL)
      }
      .as[Future[RichIndexResponse]]
      .flatMap { _ =>
        if (workflow.isActive) {
          // We update the previous version to mark it as inactive. In a normal use case, the workflow to save is
          // supposed to be active, but we never know...
          val q = boolQuery()
            .filter(termQuery("id.value", workflow.id.value))
            .filter(termQuery("is_active", 1))
            .filter(not(termQuery("version", workflow.version.get)))
          client
            .execute {
              updateIn(indexName)
                .query(q)
                .script(ScriptDefinition("ctx._source.is_active = 0;", Some("painless")))
                .refresh(true)
            }
            .as[Future[BulkIndexByScrollResponse]]
        } else {
          Future.value(true)
        }
      }
      .flatMap { _ =>
        // We need to refresh the indices because of a lot of edge cases that can happen if not. We could ultimately
        // end up with two active versions of a workflow, which is not exactly good and would mess up things when
        // looking for workflows.
        // It is not ideal, but I expect this not to be too grave, as workflows are not expected to be updated several
        // times per second.
        client
          .execute(refreshIndex(indexName))
          .as[Future[RefreshResponse]]
      }
      .onFailure(e => logger.error(s"Error while saving workflow ${workflow.id.value}", e))
      .unit
    Await.result(f, timeout)
  }

  override def transactional[T](id: WorkflowId)(fn: Option[Workflow] => T): T = locked(id.value) {
    fn(get(id))
  }

  private def internalId(id: WorkflowId, version: String) = s"${id.value}:$version"

  override protected def indexName = s"${prefix}workflows"

  override protected def typeName = s"default"

  override protected def createMappings(): Future[CreateIndexResponse] = {
    // Some fields must absolutely be indexed with the keyword analyzer, which performs no tokenization at all,
    // otherwise they won't be searchable by their exact value (which can be annoying, e.g., for ids). Graph is
    // not indexed.
    val fields = Seq(
      objectField("id").as(textField("value").analyzer(KeywordAnalyzer)),
      textField("version").analyzer(KeywordAnalyzer),
      longField("created_at"),
      booleanField("is_active"),
      objectField("graph").enabled(false),
      nestedField("params").as(
        textField("name").analyzer(KeywordAnalyzer),
        objectField("value").enabled(false)
      ))
    client
      .execute(createIndex(indexName).mappings(new MappingDefinition(typeName) as (fields: _*)))
      .as[Future[CreateIndexResponse]]
  }
}
