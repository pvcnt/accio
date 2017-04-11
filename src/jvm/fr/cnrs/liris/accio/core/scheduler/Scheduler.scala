/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.scheduler

import fr.cnrs.liris.accio.core.api._

trait Scheduler {
  def submit(task: Task): Unit

  @throws[InvalidTaskException]
  def kill(id: TaskId): Unit

  def kill(id: RunId): Set[Task]

  /**
   * Perform housekeeping operation. It is called after each operation that leads to a cluster state change.
   * For instance, it is the place where to check for new available resources.
   *
   * @param kind
   */
  def houseKeeping(kind: EventType): Unit = {}

  def close(): Unit = {}
}

sealed trait EventType

object EventType {
  case object MoreResource extends EventType

  case object LessResource extends EventType
}