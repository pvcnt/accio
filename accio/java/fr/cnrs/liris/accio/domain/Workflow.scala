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

package fr.cnrs.liris.accio.domain

import fr.cnrs.liris.lumos.domain.{AttrValue, Value}

case class Workflow(
  name: String = "",
  owner: Option[String] = None,
  contact: Option[String] = None,
  labels: Map[String, String] = Map.empty,
  seed: Long = 0,
  params: Seq[AttrValue] = Seq.empty,
  steps: Seq[Step] = Seq.empty,
  repeat: Int = 1,
  resources: Map[String, Long] = Map.empty)

case class Step(op: String, name: String = "", params: Seq[Channel] = Seq.empty)

case class Channel(name: String, source: Channel.Source)

object Channel {

  sealed trait Source

  case class Reference(step: String, output: String) extends Source

  case class Param(name: String) extends Source

  case class Constant(value: Value) extends Source

}