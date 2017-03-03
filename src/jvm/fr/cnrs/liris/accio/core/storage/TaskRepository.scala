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

package fr.cnrs.liris.accio.core.storage

import com.twitter.util.Time
import fr.cnrs.liris.accio.core.domain._

/**
 */
trait TaskRepository {
  def find(query: TaskQuery): Seq[Task]

  def get(id: TaskId): Option[Task]
}

/**
 */
trait MutableTaskRepository extends TaskRepository {
  def save(task: Task): Unit

  def remove(id: TaskId): Unit
}

/**
 * Query to search for tasks.
 *
 * @param runs   Only include tasks belonging to one of given runs.
 * @param lostAt Only include running tasks for which no heartbeat has been received before a given time.
 */
case class TaskQuery(runs: Set[RunId] = Set.empty, lostAt: Option[Time] = None) {
  def matches(task: Task): Boolean = {
    if (runs.nonEmpty && !runs.contains(task.runId)) {
      false
    } else if (lostAt.isDefined && (task.state.status != TaskStatus.Running || task.state.heartbeatAt.exists(_ >= lostAt.get.inMillis))) {
      false
    } else {
      true
    }
  }
}