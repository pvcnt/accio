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
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.search.sort.SortOrder

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

/**
 * Run repository persisting data into an Elasticsearch cluster.
 *
 * @param mapper       Finatra object mapper.
 * @param client       Elasticsearch client.
 * @param prefix       Prefix of indices managed by Accio.
 * @param queryTimeout Timeout of queries sent to Elasticsearch.
 */
final class ElasticRunRepository(
  mapper: FinatraObjectMapper,
  client: ElasticClient,
  prefix: String,
  queryTimeout: Duration)
  extends RunRepository with StrictLogging {

  override def find(query: RunQuery): RunList = {
    var q = boolQuery()
    query.cluster.foreach { cluster =>
      q = q.must(termQuery("cluster", cluster))
    }
    query.environment.foreach { environment =>
      q = q.must(termQuery("environment", environment))
    }
    query.owner.foreach { owner =>
      q = q.filter(termQuery("owner.name", owner))
    }
    query.status.foreach { status =>
      q = q.filter(termQuery("state.status", status.value))
    }
    query.workflow.foreach { workflowId =>
      q = q.filter(termQuery("pkg.workflow_id.value", workflowId.value))
    }
    query.name.foreach { name =>
      q = q.must(matchQuery("name", name))
    }

    val s = search(runsIndex / runsType)
      .query(q)
      .sortBy(fieldSort("created_at").order(SortOrder.DESC))
      .limit(query.limit)
      .from(query.offset.getOrElse(0))

    val f = client.execute(s).map { resp =>
      val results = resp.hits.toSeq.map(hit => mapper.parse[Run](hit.sourceAsBytes))
      RunList(results, resp.totalHits.toInt)
    }.recover {
      case _: IndexNotFoundException => RunList(Seq.empty, 0)
      case NonFatal(e) =>
        logger.error("Error while searching runs", e)
        RunList(Seq.empty, 0)
    }
    Await.result(f, queryTimeout)
  }

  override def find(query: LogsQuery): Seq[RunLog] = {
    val q = boolQuery()
      .must(termQuery("node_name", query.nodeName))
      .must(termQuery("run_id", query.runId.value))
    query.classifier.foreach(classifier => q.must(termQuery("classifier", classifier)))
    query.since.foreach(since => q.must(rangeQuery("created_at") from since))

    val s = search(logsIndex / logsType)
      .query(q)
      .sortBy(fieldSort("created_at"))
      .limit(query.limit)

    val f = client.execute(s).map { resp =>
      resp.hits.toSeq.map(hit => mapper.parse[RunLog](hit.sourceAsBytes))
    }.recover {
      case _: IndexNotFoundException => Seq.empty
      case NonFatal(e) =>
        logger.error("Error while searching logs", e)
        Seq.empty
    }
    Await.result(f, queryTimeout)
  }

  override def save(run: Run): Unit = {
    val json = mapper.writeValueAsString(run)
    val f = client.execute {
      indexInto(runsIndex / runsType) id run.id.value source json
    }
    f.onSuccess {
      case _ => logger.debug(s"Saved run ${run.id.value}")
    }
    f.onFailure {
      case e: Throwable => logger.error(s"Error while saving run ${run.id.value}", e)
    }
    Await.ready(f, queryTimeout)
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
    Await.ready(f, queryTimeout)
  }

  override def get(id: RunId): Option[Run] = {
    val f = client.execute {
      ElasticDsl.get(id.value).from(runsIndex / runsType)
    }.map { resp =>
      if (resp.isSourceEmpty) {
        None
      } else {
        Some(mapper.parse[Run](resp.sourceAsString))
      }
    }.recover {
      case _: IndexNotFoundException => None
      case e: Throwable =>
        logger.error(s"Error while retrieving run ${id.value}", e)
        None
    }
    Await.result(f, queryTimeout)
  }

  override def contains(id: RunId): Boolean = {
    val f = client.execute {
      ElasticDsl.get(id.value).from(runsIndex / runsType)
    }.map { resp =>
      !resp.isSourceEmpty
    }.recover {
      case _: IndexNotFoundException => false
      case e: Throwable =>
        logger.error(s"Error while retrieving run ${id.value}", e)
        false
    }
    Await.result(f, queryTimeout)
  }

  override def remove(id: RunId): Unit = {
    val f = client.execute {
      delete(id.value).from(runsIndex / runsType)
    }.flatMap { _ =>
      client.execute {
        deleteIn(logsIndex).by(termQuery("run_id.value", id.value))
      }
    }
    f.onSuccess {
      case _ => logger.debug(s"Removed run ${id.value}")
    }
    Await.ready(f, queryTimeout)
  }

  private def runsIndex = s"${prefix}runs"

  private def logsIndex = s"${prefix}logs"

  private def runsType = "default"

  private def logsType = "default"
}
