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

package fr.cnrs.liris.accio.testing

import java.util.UUID

import fr.cnrs.liris.accio.core.domain._

import scala.collection.Map

object Runs {
  val Foobar = Run(
    id = RunId("foobar"),
    pkg = Package(WorkflowId("my_workflow"), "v1"),
    owner = User("me"),
    name = Some("foo bar workflow"),
    notes = Some("awesome workflow!"),
    tags = Set("foo", "bar"),
    seed = 1234,
    params = Map.empty,
    createdAt = System.currentTimeMillis(),
    state = RunState(status = RunStatus.Scheduled, progress = 0))

  val Foo = Run(
    id = RunId("foo"),
    pkg = Package(WorkflowId("my_workflow"), "v1"),
    owner = User("me"),
    name = Some("foo bar workflow"),
    tags = Set("foo"),
    seed = 54321,
    params = Map.empty,
    createdAt = System.currentTimeMillis() - 1000,
    state = RunState(status = RunStatus.Running, progress = .5))

  def randomId: RunId = RunId(UUID.randomUUID().toString)
}