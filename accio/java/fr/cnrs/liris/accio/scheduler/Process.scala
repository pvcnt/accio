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

package fr.cnrs.liris.accio.scheduler

import fr.cnrs.liris.accio.api.thrift.OpPayload

case class Process(name: String, payload: OpPayload) {
  def jobName: String = name match {
    case Process.NameRegex(v, _) => v
    case _ => throw new IllegalStateException(s"Malformed process name: $name")
  }

  def taskName: String = name match {
    case Process.NameRegex(_, v) => v
    case _ => throw new IllegalStateException(s"Malformed process name: $name")
  }
}

object Process {
  private val NameRegex = "^accio_job_([^-]+)_([^-]+)$".r

  def name(jobName: String, taskName: String): String = s"accio_job_${jobName}_$taskName"
}