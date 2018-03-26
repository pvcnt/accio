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

package fr.cnrs.liris.accio.storage.memory

import java.util.concurrent.ConcurrentHashMap

import com.google.common.annotations.VisibleForTesting
import com.google.common.util.concurrent.AbstractIdleService
import com.google.inject.Singleton
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.storage.{MutableRunRepository, RunList, RunQuery}
import fr.cnrs.liris.accio.util.Lockable

import scala.collection.JavaConverters._

/**
 * Run repository storing data in memory. It has no persistence mechanism. Intended for testing only.
 */
@Singleton
private[memory] final class MemoryRunRepository extends AbstractIdleService with MutableRunRepository with Lockable[String] {
  private[this] val index = new ConcurrentHashMap[RunId, Run]().asScala

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

  override def save(run: Run): Unit = locked(run.id.value) {
    index(run.id) = run
  }

  override def remove(id: RunId): Unit = locked(id.value) {
    index.remove(id)
  }

  override def transactional[T](id: RunId)(fn: Option[Run] => T): T = locked(id.value) {
    fn(get(id))
  }

  override protected def shutDown(): Unit = {}

  override protected def startUp(): Unit = {}
}