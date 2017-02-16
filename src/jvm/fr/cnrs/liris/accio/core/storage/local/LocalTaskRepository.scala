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

package fr.cnrs.liris.accio.core.storage.local

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.storage._
import fr.cnrs.liris.common.util.FileUtils

/**
 * @param config Local repository configuration.
 */
@Singleton
final class LocalTaskRepository @Inject()(config: LocalStorageConfig)
  extends LocalRepository with MutableTaskRepository {

  override def save(task: Task): Unit = {
    write(task, taskPath(task.id))
  }

  override def remove(id: TaskId): Unit = {
    FileUtils.safeDelete(taskPath(id))
  }

  override def find(query: TaskQuery): Seq[Task] = {
    taskPath.toFile
      .listFiles
      .toSeq
      .filter(_.isFile)
      .flatMap(file => get(TaskId(file.getName)))
      .filter(query.matches)
  }

  override def get(id: TaskId): Option[Task] = read(taskPath(id), Task)

  private def taskPath = config.path.resolve("tasks")

  private def taskPath(id: TaskId) = taskPath.resolve(id.value)
}