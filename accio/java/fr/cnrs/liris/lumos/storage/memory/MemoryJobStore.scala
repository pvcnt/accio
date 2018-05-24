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

package fr.cnrs.liris.lumos.storage.memory

import java.util.concurrent.ConcurrentHashMap

import com.github.nscala_time.time.Imports._
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.util.{Future, Time}
import fr.cnrs.liris.lumos.domain.{Job, JobList, Status}
import fr.cnrs.liris.lumos.storage.{JobQuery, JobStore}

import scala.collection.JavaConverters._

/**
 * Run repository storing data in memory.
 *
 * @param statsReceiver Stats receiver.
 */
private[storage] final class MemoryJobStore(statsReceiver: StatsReceiver) extends JobStore {
  private[this] val index = new ConcurrentHashMap[String, Job].asScala
  statsReceiver.provideGauge("storage", "job", "index_size")(index.size)

  override def list(query: JobQuery, limit: Option[Int], offset: Option[Int]): Future[JobList] = {
    val results = index.values
      .filter(query.matches)
      .toSeq
      .sortWith((a, b) => a.createTime > b.createTime)
    Future.value(JobList.slice(results, offset = offset, limit = limit))
  }

  override def get(name: String): Future[Option[Job]] = Future.value(index.get(name))

  override def create(job: Job): Future[Status] = {
    if (index.putIfAbsent(job.name, job).isEmpty) {
      Future.value(Status.Ok)
    } else {
      Future.value(Status.AlreadyExists(job.name))
    }
  }

  override def replace(job: Job): Future[Status] = {
    if (index.replace(job.name, job).isDefined) {
      Future.value(Status.Ok)
    } else {
      Future.value(Status.NotFound(job.name))
    }
  }

  override def delete(name: String): Future[Status] = {
    if (index.remove(name).isDefined) {
      Future.value(Status.Ok)
    } else {
      Future.value(Status.NotFound(name))
    }
  }

  override def startUp(): Future[Unit] = Future.Done

  override def close(deadline: Time): Future[Unit] = Future.Done
}

object MemoryJobStore {
  /**
   * Creates a new empty in-memory job store for use in testing.
   */
  def empty: JobStore = new MemoryJobStore(NullStatsReceiver)
}