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

import java.util.concurrent.ConcurrentHashMap

import com.google.common.annotations.VisibleForTesting
import com.google.common.util.concurrent.AbstractIdleService
import com.google.inject.Singleton
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.accio.core.storage.{MutableRunRepository, RunList, RunQuery}

import scala.collection.JavaConverters._

/**
 * Run repository storing data in memory. It has no persistence mechanism. Intended for testing only.
 */
@Singleton
@VisibleForTesting
final class MemoryRunRepository extends AbstractIdleService with MutableRunRepository {
  private[this] val index = new ConcurrentHashMap[RunId, Run]().asScala

  override def save(run: Run): Unit = {
    index(run.id) = run
  }

  override def remove(id: RunId): Unit = {
    index.remove(id)
  }

  override def find(query: RunQuery): RunList = {
    var results = index.values
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

  override def get(id: RunId): Option[Run] = index.get(id)

  override def get(cacheKey: CacheKey): Option[OpResult] = None

  override protected def shutDown(): Unit = {}

  override protected def startUp(): Unit = {}
}