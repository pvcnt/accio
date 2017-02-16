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

package fr.cnrs.liris.accio.core.storage.elastic

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers.KeywordAnalyzer
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl}
import com.twitter.finatra.json.FinatraObjectMapper
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.storage._
import org.elasticsearch.index.IndexNotFoundException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.language.reflectiveCalls
import scala.util.control.NonFatal

/**
 * Run repository persisting data into an Elasticsearch cluster.
 *
 * @param mapper Finatra object mapper.
 * @param client Elasticsearch client.
 * @param config Elastic repository configuration.
 */
@Singleton
final class ElasticTaskRepository @Inject()(
  @ForStorage mapper: FinatraObjectMapper,
  @ForStorage client: ElasticClient,
  config: StorageConfig)
  extends MutableTaskRepository with StrictLogging {

  initializeTasksIndex()

  override def find(query: TaskQuery): Seq[Task] = {
    var q = boolQuery()
    if (query.runs.nonEmpty) {
      q = q.filter(termsQuery("run_id.value", query.runs.map(_.value)))
    }
    query.lostAt.foreach { lostAt =>
      q = q
        .filter(termQuery("state.status", TaskStatus.Running.value))
        .filter(boolQuery()
          .should(not(existsQuery("state.heartbeat_at")))
          .should(rangeQuery("state.heartbeat_at").to(lostAt.inMillis).includeUpper(false))
          .minimumShouldMatch(1))
    }

    val s = search(taskIndex / taskType)
      .query(q)
      .limit(10000) // Max limit defaults to 10000.

    val f = client.execute(s).map { resp =>
      resp.hits.toSeq.map(hit => mapper.parse[Task](hit.sourceAsBytes))
    }.recover {
      case _: IndexNotFoundException => Seq.empty[Task]
      case NonFatal(e) =>
        logger.error("Error while searching tasks", e)
        Seq.empty[Task]
    }
    Await.result(f, config.queryTimeout)
  }

  override def save(task: Task): Unit = {
    val json = mapper.writeValueAsString(task)
    val f = client.execute(indexInto(taskIndex / taskType).id(task.id.value).source(json))
    f.onFailure {
      case e: Throwable => logger.error(s"Error while saving task ${task.id.value}", e)
    }
    Await.ready(f, config.queryTimeout)
  }

  override def get(id: TaskId): Option[Task] = {
    val f = client.execute(ElasticDsl.get(id.value).from(taskIndex / taskType))
      .map { resp =>
        if (resp.isSourceEmpty) {
          None
        } else {
          Some(mapper.parse[Task](resp.sourceAsString))
        }
      }
      .recover {
        case _: IndexNotFoundException => None
        case e: Throwable =>
          logger.error(s"Error while retrieving task ${id.value}", e)
          None
      }
    Await.result(f, config.queryTimeout)
  }

  override def remove(id: TaskId): Unit = {
    val f = client.execute(delete(id.value).from(taskIndex / taskType))
    f.onSuccess {
      case _ => logger.debug(s"Removed run ${id.value}")
    }
    Await.ready(f, config.queryTimeout)
  }

  private def taskIndex = s"${config.prefix}task"

  private def taskType = "default"

  private def initializeTasksIndex() = {
    val f = client.execute(indexExists(taskIndex)).flatMap { resp =>
      if (!resp.isExists) {
        // Some fields must absolutely be indexed with the keyword analyzer, which performs no tokenization at all,
        // otherwise they won't be searchable by their exact value (which can be annoying, e.g., for ids).
        val fields = Seq(
          objectField("id").as(textField("value").analyzer(KeywordAnalyzer)),
          objectField("run_id").as(textField("value").analyzer(KeywordAnalyzer)),
          textField("key").analyzer(KeywordAnalyzer),
          longField("created_at"),
          objectField("payload").enabled(false)
        )
        logger.info(s"Creating $taskIndex/$taskType index")
        client.execute(createIndex(taskIndex).mappings(new MappingDefinition(taskType) as (fields: _*)))
      } else {
        Future.successful(true)
      }
    }
    f.onFailure { case NonFatal(e) => logger.error("Failed to initialize tasks index", e) }
    Await.ready(f, config.queryTimeout)
  }
}
