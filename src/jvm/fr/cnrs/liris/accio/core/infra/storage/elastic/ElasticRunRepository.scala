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

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl}
import com.twitter.finatra.json.FinatraObjectMapper
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

final class ElasticRunRepository(mapper: FinatraObjectMapper, client: ElasticClient, prefix: String)
  extends RunRepository with StrictLogging {

  override def find(query: RunQuery): RunList = {
    /*val qb = boolQuery()
    query.cluster.foreach(cluster => qb.filter(termQuery("cluster", cluster)))
    query.environment.foreach(environment => qb.filter(termQuery("environment", environment)))
    query.owner.foreach(owner => qb.filter(termQuery("owner.name", owner)))
    query.status.foreach(status => qb.filter(termQuery("state.status", status)))
    query.workflow.foreach(workflowId => qb.filter(termQuery("pkg.workflowId", workflowId)))
    query.name.foreach(name => qb.must(matchQuery("name", name)))

    val searchQuery = client.prepareSearch(runsIndex)
      .setTypes(runsType)
      .setQuery(qb)
      .addSort("createdAt", SortOrder.DESC)
    query.limit.foreach(searchQuery.setSize)
    query.offset.foreach(searchQuery.setFrom)

    val resp = searchQuery.get()
    val results = resp.getHits.hits.toSeq.map(hit => mapper.parse[Run](hit.getSourceAsString))
    RunList(results, resp.getHits.totalHits.toInt)*/
    ???
  }

  override def find(query: LogsQuery): Seq[RunLog] = {
    //.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
    /*val qb = boolQuery()
      .filter(termQuery("nodeName", query.nodeName))
      .filter(termQuery("runId", query.runId.value))
    query.classifier.foreach(classifier => qb.filter(termQuery("classifier", classifier)))
    query.since.foreach(since => qb.filter(rangeQuery("createdAt").from(since)))

    val searchQuery = client.prepareSearch(logsIndex)
      .setTypes(logsType)
      .setQuery(qb)
      .addSort("createdAt", SortOrder.DESC)
    query.limit.foreach(searchQuery.setSize)

    val resp = searchQuery.get()
    resp.getHits.hits.toSeq.map(hit => mapper.parse[RunLog](hit.getSourceAsString))*/
    Seq.empty
  }

  override def save(run: Run): Unit = {
    val json = mapper.writeValueAsString(run)
    val f = client.execute {
      indexInto(runsIndex / runsType) id run.id.value source json
    }
    f.onFailure {
      case e: Throwable => logger.error(s"Error while saving run ${run.id.value}", e)
    }
    Await.result(f, Duration.Inf)
  }

  override def save(logs: Seq[RunLog]): Unit = {
    val actions = logs.map { log =>
      val json = mapper.writeValueAsString(log)
      indexInto(logsIndex / logsType) source json
    }
    val f = client.execute(bulk(actions: _*))
    f.onFailure {
      case e: Throwable => logger.error(s"Error while saving ${logs.size} logs", e)
    }
    Await.result(f, Duration.Inf)
  }

  override def get(id: RunId): Option[Run] = {
    val f = client.execute {
      ElasticDsl.get(id.value).from(runsIndex / runsType)
    }.map { resp =>
      try {
        Some(mapper.parse[Run](resp.sourceAsString))
      } catch {
        case NonFatal(e) =>
          logger.error(s"Error while decoding run ${id.value}", e)
          None
      }
    }.recover {
      case e: Throwable =>
        logger.error(s"Error while retrieving run ${id.value}", e)
        None
    }
    Await.result(f, Duration.Inf)
  }

  override def exists(id: RunId): Boolean = {
    val f = client.execute {
      ElasticDsl.get(id.value).from(runsIndex / runsType)
    }.map { _ => true }
    .recover { case e: Throwable => false }
    Await.result(f, Duration.Inf)
  }

  override def remove(id: RunId): Unit = {
    client.execute {
      delete(id.value).from(runsIndex / runsType)
    }
    client.execute {
      deleteIn(logsIndex).by(termQuery("run_id", id.value))
    }
  }

  private def runsIndex = s"${prefix}__runs"

  private def logsIndex = s"${prefix}__logs"

  private def runsType = "default"

  private def logsType = "default"
}
