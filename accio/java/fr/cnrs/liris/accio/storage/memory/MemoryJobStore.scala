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
import com.twitter.util.Future
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.storage.{JobStore, JobList}

import scala.collection.JavaConverters._

/**
 * Run repository storing data in memory.
 *
 * @param statsReceiver Stats receiver.
 */
private[memory] final class MemoryJobStore(statsReceiver: StatsReceiver) extends JobStore {
  private[this] val index = new ConcurrentHashMap[String, Job].asScala
  statsReceiver.provideGauge("storage", "memory", "job", "index_size")(index.size)

  override def list(query: JobStore.Query, limit: Option[Int], offset: Option[Int]): Future[JobList] =
    Future {
      val results = index.values
        .filter(query.matches)
        .toSeq
        .sortWith((a, b) => a.createTime > b.createTime)
      JobList.slice(results, offset = offset, limit = limit)
    }

  override def get(name: String): Future[Option[Job]] = Future(index.get(name))

  override def create(job: Job): Future[Boolean] = Future(index.putIfAbsent(job.name, job).isEmpty)

  override def replace(job: Job): Future[Boolean] = Future(index.replace(job.name, job).isDefined)

  override def delete(name: String): Future[Boolean] = Future(index.remove(name).isDefined)
}