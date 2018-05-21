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

  final def startTime: Option[Instant] = {
    val idx = history.lastIndexWhere(_ == ExecStatus.Running)
    if (idx > -1) Some(history(idx).time) else None
  }

  final def endTime: Option[Instant] = {
    val startAt = history.lastIndexWhere(_ == ExecStatus.Running)
    if (startAt > -1) {
      val idx = history.indexWhere(_.state.isCompleted, startAt)
      if (idx > -1) Some(history(idx).time) else None
    } else {
      None
    }
  }

  final def duration: Duration = {
    val startAt = history.lastIndexWhere(_ == ExecStatus.Running)
    if (startAt > -1) {
      val endAt = history.indexWhere(_.state.isCompleted, startAt)
      if (endAt > -1) {
        new Duration(history(startAt).time, history(endAt).time)
      } else {
        new Duration(history(startAt).time, Instant.now())
      }
    } else {
      Duration.ZERO
    }
  }
}
