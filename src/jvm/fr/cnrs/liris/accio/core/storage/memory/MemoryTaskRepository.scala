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

package fr.cnrs.liris.accio.core.storage.memory

import java.util.concurrent.ConcurrentHashMap

import com.google.inject.Singleton
import fr.cnrs.liris.accio.core.domain.{Task, TaskId}
import fr.cnrs.liris.accio.core.storage.{MutableTaskRepository, TaskQuery}

import scala.collection.JavaConverters._

/**
 * Task repository storing data in memory. It has no persistence mechanism. Intended for testing only.
 */
@Singleton
final class MemoryTaskRepository extends MutableTaskRepository {
  private[this] val taskIndex = new ConcurrentHashMap[TaskId, Task]().asScala

  override def save(task: Task): Unit = {
    taskIndex(task.id) = task
  }

  override def remove(id: TaskId): Unit = {
    taskIndex.remove(id)
  }

  override def find(query: TaskQuery): Seq[Task] = {
    taskIndex.values.toSeq.filter(query.matches)
  }

  override def get(id: TaskId): Option[Task] = taskIndex.get(id)
}
