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

package fr.cnrs.liris.accio.framework.storage.memory

import java.util
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue}

import com.google.common.annotations.VisibleForTesting
import com.google.common.util.concurrent.AbstractIdleService
import com.google.inject.Singleton
import fr.cnrs.liris.accio.framework.api.thrift._
import fr.cnrs.liris.accio.framework.storage._

import scala.collection.JavaConverters._

/**
 * Run repository storing data in memory. It has no persistence mechanism. Intended for testing only.
 */
@Singleton
@VisibleForTesting
final class MemoryLogRepository extends AbstractIdleService with MutableLogRepository {
  private[this] val index = new ConcurrentHashMap[RunId, util.Queue[RunLog]]().asScala

  override def save(logs: Seq[RunLog]): Unit = {
    logs.groupBy(_.runId).foreach { case (runId, logs) =>
      index
        .getOrElseUpdate(runId, new ConcurrentLinkedQueue[RunLog])
        .addAll(logs.asJava)
    }
  }

  override def remove(id: RunId): Unit = {
    index.remove(id)
  }

  override def find(query: LogsQuery): Seq[RunLog] = {
    var results = index(query.runId).asScala
      .filter(query.matches)
      .toSeq
      .sortWith((a, b) => a.createdAt < b.createdAt)
    query.limit.foreach { limit =>
      results = results.take(limit)
    }
    results
  }

  override protected def shutDown(): Unit = {}

  override protected def startUp(): Unit = {}
}
