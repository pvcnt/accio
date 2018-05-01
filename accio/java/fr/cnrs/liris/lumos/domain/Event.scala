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

package fr.cnrs.liris.lumos.domain

import org.joda.time.Instant

case class Event(parent: String, sequence: Long, time: Instant, payload: Event.Payload)

object Event {

  sealed trait Payload

  case class JobEnqueued(job: Job) extends Payload

  case class JobExpanded(tasks: Seq[Task]) extends Payload

  case class JobStarted(message: Option[String] = None) extends Payload

  case class JobCanceled(message: Option[String] = None) extends Payload

  case class JobCompleted(
    outputs: Seq[AttrValue] = Seq.empty,
    message: Option[String] = None)
    extends Payload

  case class TaskStarted(
    name: String,
    links: Seq[Link] = Seq.empty,
    message: Option[String] = None)
    extends Payload

  case class TaskCompleted(
    name: String,
    exitCode: Int,
    metrics: Seq[MetricValue] = Seq.empty,
    error: Option[ErrorDatum] = None,
    message: Option[String] = None)
    extends Payload

}