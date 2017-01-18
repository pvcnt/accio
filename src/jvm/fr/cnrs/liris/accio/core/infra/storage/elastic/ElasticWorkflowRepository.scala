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

package fr.cnrs.liris.accio.core.infra.storage.elastic

import com.sksamuel.elastic4s.ElasticDsl.{search, _}
import com.sksamuel.elastic4s.analyzers.KeywordAnalyzer
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.sksamuel.elastic4s.script.ScriptDefinition
import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl}
import com.twitter.finatra.json.FinatraObjectMapper
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain._
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.search.sort.SortOrder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

/**
 * Workflow repository persisting data into an Elasticsearch cluster.
 *
 * @param mapper       Finatra object mapper.
 * @param client       Elasticsearch client.
 * @param prefix       Prefix of indices managed by Accio.
 * @param queryTimeout Timeout of queries sent to Elasticsearch.
 */
class ElasticWorkflowRepository(
  mapper: FinatraObjectMapper,
  client: ElasticClient,
  prefix: String,
  queryTimeout: Duration)
  extends WorkflowRepository with StrictLogging {

  initializeWorkflowsIndex()

  override def find(query: WorkflowQuery): WorkflowList = {
    var q = boolQuery().filter(termQuery("is_active", 1))
    query.owner.foreach { owner =>
      q = q.filter(termQuery("owner.name", owner))
    }
    query.name.foreach { name =>
      q = q.must(matchQuery("name", name))
    }

    val s = search(workflowsIndex / workflowsType)
      .query(q)
      .sortBy(fieldSort("created_at").order(SortOrder.DESC))
      .limit(query.limit)
      .from(query.offset.getOrElse(0))

    val f = client.execute(s).map { resp =>
      val results = resp.hits.toSeq.map(hit => mapper.parse[Workflow](hit.sourceAsBytes))
      WorkflowList(results, resp.totalHits.toInt)
    }.recover {
      case _: IndexNotFoundException => WorkflowList(Seq.empty, 0)
      case NonFatal(e) =>
        logger.error("Error while searching workflows", e)
        WorkflowList(Seq.empty, 0)
    }
    Await.result(f, queryTimeout)
  }

  override def save(workflow: Workflow): Unit = {
    val json = mapper.writeValueAsString(workflow)
    val f = client.execute {
      // We insert the new version.
      indexInto(workflowsIndex / workflowsType)
        .id(internalId(workflow.id, workflow.version))
        .source(json)
        .refresh(RefreshPolicy.WAIT_UNTIL)
    }.flatMap { _ =>
      if (workflow.isActive) {
        // We update the previous version to mark it as inactive. In a normal use case, the workflow to save is
        // supposed to be active, but we never know...
        val q = boolQuery()
          .filter(termQuery("id.value", workflow.id.value))
          .filter(termQuery("is_active", 1))
          .filter(not(termQuery("version", workflow.version)))
        client.execute {
          updateIn(workflowsIndex)
            .query(q)
            .script(ScriptDefinition("ctx._source.is_active = 0;", Some("painless")))
            .refresh(true)
        }
      } else {
        Future.successful(true)
      }
    }.flatMap { _ =>
      // We need to refresh the indices because of a lot of edge cases that can happen if not. We could ultimately
      // end up with two active versions of a workflow, which is not exactly good and would mess up things when
      // looking for workflows.
      // It is not ideal, but I expect this not to be too grave, as workflows are not expected to be updated several
      // times per second.
      client.execute(refreshIndex(workflowsIndex))
    }
    f.onSuccess {
      case _ => logger.debug(s"Saved workflow ${workflow.id.value}:${workflow.version}")
    }
    f.onFailure {
      case e: Throwable => logger.error(s"Error while saving workflow ${workflow.id.value}", e)
    }
    Await.ready(f, queryTimeout)
  }

  override def get(id: WorkflowId): Option[Workflow] = {
    val q = boolQuery()
      .filter(termQuery("id.value", id.value))
      .filter(termQuery("is_active", 1))
    val f = client.execute {
      search(workflowsIndex / workflowsType).query(q).limit(1)
    }.map { resp =>
      if (resp.totalHits > 0) {
        Some(mapper.parse[Workflow](resp.hits.head.sourceAsString))
      } else {
        None
      }
    }.recover {
      case _: IndexNotFoundException => None
      case e: Throwable =>
        logger.error(s"Error while retrieving workflow ${id.value}", e)
        None
    }
    Await.result(f, queryTimeout)
  }

  override def get(id: WorkflowId, version: String): Option[Workflow] = {
    val f = client.execute {
      ElasticDsl.get(internalId(id, version)).from(workflowsIndex / workflowsType)
    }.map { resp =>
      if (resp.isSourceEmpty) {
        None
      } else {
        Some(mapper.parse[Workflow](resp.sourceAsString))
      }
    }.recover {
      case _: IndexNotFoundException => None
      case e: Throwable =>
        logger.error(s"Error while retrieving workflow ${id.value}", e)
        None
    }
    Await.result(f, queryTimeout)
  }

  override def contains(id: WorkflowId): Boolean = {
    val f = client.execute {
      search(workflowsIndex / workflowsType).query(termQuery("id.value", id.value)).size(1)
    }.map { resp =>
      resp.totalHits > 0
    }.recover {
      case _: IndexNotFoundException => false
      case e: Throwable =>
        logger.error(s"Error while retrieving workflow ${id.value}", e)
        false
    }
    Await.result(f, queryTimeout)
  }

  override def contains(id: WorkflowId, version: String): Boolean = {
    val f = client.execute {
      ElasticDsl.get(s"${id.value}:$version").from(workflowsIndex / workflowsType)
    }.map { resp =>
      !resp.isSourceEmpty
    }.recover {
      case _: Throwable => false
    }
    Await.result(f, queryTimeout)
  }

  private def internalId(id: WorkflowId, version: String) = s"${id.value}:$version"

  private def workflowsIndex = s"${prefix}runs"

  private def workflowsType = s"default"

  private def initializeWorkflowsIndex() = {
    val f = client.execute(indexExists(workflowsIndex)).flatMap { resp =>
      if (!resp.isExists) {
        // Some fields must absolutely be indexed with the keyword analyzer, which performs no tokenization at all,
        // otherwise they won't be searchable by their exact value (which can be annoying, e.g., for ids).
        val fields = Seq(
          objectField("id").as(textField("value").analyzer(KeywordAnalyzer)),
          textField("version").analyzer(KeywordAnalyzer))
        logger.info(s"Creating $workflowsIndex/$workflowsType index")
        client.execute(createIndex(workflowsIndex).mappings(new MappingDefinition(workflowsType) as (fields: _*)))
      } else {
        Future.successful(true)
      }
    }
    f.onFailure { case NonFatal(e) => logger.error("Failed to initialize logs index", e) }
    Await.ready(f, queryTimeout)
  }
}