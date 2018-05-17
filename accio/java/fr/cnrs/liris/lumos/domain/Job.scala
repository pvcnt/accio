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

case class Job(
  name: String = "",
  createTime: Instant = new Instant(0),
  owner: Option[String] = None,
  contact: Option[String] = None,
  labels: Map[String, String] = Map.empty,
  metadata: Map[String, String] = Map.empty,
  inputs: Seq[AttrValue] = Seq.empty,
  outputs: Seq[AttrValue] = Seq.empty,
  progress: Int = 0,
  tasks: Seq[Task] = Seq.empty,
  status: ExecStatus = ExecStatus(),
  history: Seq[ExecStatus] = Seq.empty)
  extends StatusHolder

object Job {
  val empty = Job()
}