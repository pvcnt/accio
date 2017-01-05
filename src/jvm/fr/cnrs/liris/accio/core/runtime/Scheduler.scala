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

package fr.cnrs.liris.accio.core.runtime

import java.util.UUID

import com.twitter.finatra.domain.WrappedValue
import fr.cnrs.liris.accio.core.framework.{Node, Run}
import org.joda.time.DateTime

case class JobId(value: String) extends WrappedValue[String]

object JobId {
  def random: JobId = JobId(UUID.randomUUID().toString)
}

case class Job(id: JobId, run: Run, node: Node)

case class JobStatus(createdAt: DateTime, startedAt: Option[DateTime], completedAt: Option[DateTime], exitCode: Option[Int], detailsUrl: Option[String])

trait Scheduler {
  def schedule(jobs: Seq[Job]): Unit

  def kill(id: JobId): Unit

  def stop(): Unit

  def get(id: JobId): Option[JobStatus]
}
