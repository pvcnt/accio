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

package fr.cnrs.liris.accio.core.storage.memory

import java.util
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue}

import com.google.inject.Singleton
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.storage.{LogsQuery, MutableRunRepository, RunList, RunQuery}

import scala.collection.JavaConverters._

/**
 * Run repository storing data in memory. It has no persistence mechanism. Intended for testing only.
 */
@Singleton
final class MemoryRunRepository extends MutableRunRepository {
  private[this] val runIndex = new ConcurrentHashMap[RunId, Run]().asScala
  private[this] val logIndex = new ConcurrentHashMap[RunId, util.Queue[RunLog]]().asScala

  override def save(run: Run): Unit = {
    runIndex(run.id) = run
  }

  override def save(logs: Seq[RunLog]): Unit = {
    logs.groupBy(_.runId).foreach { case (runId, logs) =>
      logIndex
        .getOrElseUpdate(runId, new ConcurrentLinkedQueue[RunLog])
        .addAll(logs.asJava)
    }
  }

  override def remove(id: RunId): Unit = {
    runIndex.remove(id)
    logIndex.remove(id)
  }

  override def find(query: RunQuery): RunList = {
    var results = runIndex.values
      .filter(query.matches)
      .toSeq
      .sortWith((a, b) => a.createdAt > b.createdAt)

    val totalCount = results.size
    query.offset.foreach { offset => results = results.drop(offset) }
    query.limit.foreach { limit => results = results.take(limit) }

    // Remove the result of each node, that we do not want to return.
    results = results.map(run => run.copy(state = run.state.copy(nodes = run.state.nodes.map(_.unsetResult))))

    RunList(results, totalCount)
  }

  override def find(query: LogsQuery): Seq[RunLog] = {
    var results = logIndex(query.runId).asScala
      .filter(query.matches)
      .toSeq
      .sortWith((a, b) => a.createdAt < b.createdAt)
    query.limit.foreach { limit =>
      results = results.take(limit)
    }
    results
  }

  override def get(id: RunId): Option[Run] = runIndex.get(id)

  override def get(cacheKey: CacheKey): Option[OpResult] = None
}
