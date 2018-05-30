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
  extends Ordered[Event] {

  override def compare(that: Event): Int = sequence.compare(that.sequence)
}

object Event {

  sealed trait Payload {
    def isTerminal: Boolean
  }

  case class JobEnqueued(job: Job) extends Payload {
    def isTerminal = false
  }

  case class JobExpanded(tasks: Seq[Task]) extends Payload {
    def isTerminal = false
  }

  case class JobScheduled(metadata: Map[String, String] = Map.empty, message: Option[String] = None)
    extends Payload {

    def isTerminal = false
  }

  case class JobStarted(message: Option[String] = None) extends Payload {
    def isTerminal = false
  }

  case class JobCanceled(message: Option[String] = None) extends Payload {
    def isTerminal = true
  }

  case class JobCompleted(outputs: Seq[AttrValue] = Seq.empty, message: Option[String] = None)
    extends Payload {

    def isTerminal = true
  }

  case class TaskScheduled(
    name: String,
    metadata: Map[String, String] = Map.empty,
    message: Option[String] = None)
    extends Payload {

    def isTerminal = false
  }

  case class TaskStarted(name: String, message: Option[String] = None) extends Payload {
    def isTerminal = false
  }

  case class TaskCompleted(
    name: String,
    exitCode: Int,
    metrics: Seq[MetricValue] = Seq.empty,
    error: Option[ErrorDatum] = None,
    message: Option[String] = None)
    extends Payload {

    def isTerminal = false
  }

}