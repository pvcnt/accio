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

case class ExecStatus(
  state: ExecStatus.State = ExecStatus.Pending,
  time: Instant = new Instant(0),
  message: Option[String] = None)

object ExecStatus {

  sealed trait State {
    def isCompleted: Boolean

    def name: String
  }

  case object Pending extends State {
    override def isCompleted = false

    override def name = "Pending"
  }

  case object Scheduled extends State {
    override def isCompleted = false

    override def name = "Scheduled"
  }

  case object Running extends State {
    override def isCompleted = false

    override def name = "Running"
  }

  case object Canceled extends State {
    override def isCompleted = true

    override def name = "Canceled"
  }

  case object Successful extends State {
    override def isCompleted = true

    override def name = "Successful"
  }

  case object Failed extends State {
    override def isCompleted = true

    override def name = "Failed"
  }

  case object Lost extends State {
    override def isCompleted = true

    override def name = "Lost"
  }

  def values: Seq[State] = Seq(Pending, Scheduled, Running, Failed, Successful, Canceled, Lost)
}