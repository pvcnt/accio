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

import org.joda.time.{Duration, Instant}

trait StatusHolder {
  def status: ExecStatus

  def history: Seq[ExecStatus]

  final def state: ExecStatus.State = status.state

  final def startTime: Option[Instant] = getLastTransition(ExecStatus.Running).map(_.time)

  final def endTime: Option[Instant] = getLastTransition(_.isCompleted, _ == ExecStatus.Running).map(_.time)

  final def duration: Duration = {
    startTime.flatMap(start => endTime.map(end => new Duration(start, end))).getOrElse(Duration.ZERO)
  }

  private def getLastTransition(to: ExecStatus.State): Option[ExecStatus] = {
    if (status.state == to) {
      Some(status)
    } else {
      val idx = history.lastIndexWhere(_.state == to)
      if (idx > -1) Some(history(idx)) else None
    }
  }

  private def getLastTransition(to: ExecStatus.State => Boolean, after: ExecStatus.State => Boolean): Option[ExecStatus] = {
    if (after(status.state)) {
      None
    } else {
      val startAt = history.lastIndexWhere(item => after(item.state))
      if (startAt > -1) {
        if (to(status.state)) {
          Some(status)
        } else {
          val idx = history.lastIndexWhere(item => to(item.state), startAt)
          if (idx > -1) Some(history(idx)) else None
        }
      } else {
        None
      }
    }
  }
}
