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

import com.twitter.finagle.stats.StatsReceiver
import fr.cnrs.liris.accio.api.ResultList
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.storage.{RunQuery, RunStore}

import scala.collection.JavaConverters._

/**
 * Run repository storing data in memory. Intended for testing only.
 *
 * @param statsReceiver Stats receiver.
 */
private[memory] final class MemoryRunStore(statsReceiver: StatsReceiver)
  extends RunStore.Mutable {

  private[this] val index = new ConcurrentHashMap[String, Run].asScala
  statsReceiver.provideGauge("storage", "memory", "run", "index_size")(index.size)

  override def list(query: RunQuery): ResultList[Run] = {
    val results = index.values
      .filter(query.matches)
      .toSeq
      .sortWith((a, b) => a.createdAt > b.createdAt)
    ResultList.slice(results, offset = query.offset, limit = query.limit)
  }

  override def get(id: String): Option[Run] = index.get(id)

  override def fetch(cacheKey: String): Option[NodeStatus] = None

  override def save(run: Run): Unit = index(run.id) = run

  override def delete(id: String): Unit = index.remove(id)
}