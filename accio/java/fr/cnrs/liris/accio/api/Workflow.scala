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

package fr.cnrs.liris.accio.api

import java.util.UUID

import fr.cnrs.liris.common.util.HashUtils
import org.joda.time.Instant

case class Workflow(
  name: String,
  version: String,
  createdAt: Instant,
  title: Option[String] = None,
  owner: Option[UserInfo] = None,
  graph: Graph = Graph(),
  params: Seq[thrift.ArgDef] = Seq.empty) {

  def toThrift: thrift.Workflow =
    thrift.Workflow(
      id = name,
      version = Some(version),
      createdAt = Some(createdAt.getMillis),
      name = title,
      owner = owner.map(_.toThrift),
      graph = graph.toThrift,
      params = params)
}

object Workflow {
  def fromThrift(struct: thrift.Workflow, user: Option[UserInfo] = None): Workflow = {
    val graph = Graph.fromThrift(struct.graph)
    val owner = user.orElse(struct.owner.map(UserInfo.fromThrift))
    val version = struct.version.getOrElse(HashUtils.sha1(UUID.randomUUID().toString))
    Workflow(
      name = struct.id,
      version = version,
      createdAt = struct.createdAt.map(millis => new Instant(millis)).getOrElse(Instant.now),
      title = struct.name,
      owner = owner,
      graph = graph,
      params = struct.params)
  }
}