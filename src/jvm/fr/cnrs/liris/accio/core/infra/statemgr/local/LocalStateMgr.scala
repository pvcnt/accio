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

package fr.cnrs.liris.accio.core.infra.statemgr.local

import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock

import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain.{Task, TaskId}
import fr.cnrs.liris.accio.core.infra.util.LocalStorage
import fr.cnrs.liris.accio.core.service.{Lock, StateManager}
import fr.cnrs.liris.common.util.FileUtils

import scala.collection.mutable

/**
 * State manager storing data on the local filesystem. Intended for testing or use in single-node development
 * clusters.
 *
 * @param rootDir Root directory under which data will be written.
 */
final class LocalStateMgr(rootDir: Path) extends LocalStorage with StateManager with StrictLogging {
  private[this] val locks = mutable.WeakHashMap.empty[String, LocalLock]

  override def lock(key: String): Lock = synchronized {
    locks.getOrElseUpdate(key, new LocalLock(key))
  }

  override def tasks: Set[Task] = {
    tasksPath.toFile.listFiles.filter(_.isFile).flatMap(file => get(TaskId(file.getName))).toSet
  }

  override def save(task: Task): Unit = {
    write(task, taskPath(task.id))
    logger.debug(s"Saved task $task")
  }

  override def remove(id: TaskId): Unit = {
    FileUtils.safeDelete(taskPath(id))
    logger.debug(s"Removed task ${id.value}")
  }

  override def get(id: TaskId): Option[Task] = read(taskPath(id), Task)

  private def tasksPath = rootDir.resolve("tasks")

  private def taskPath(id: TaskId) = tasksPath.resolve(id.value)

  /**
   * Lock implementation using local files to synchronize. It is *NOT* reentrant.
   *
   * @param key Lock key.
   */
  private class LocalLock(key: String) extends Lock {
    private[this] val javaLock = new ReentrantLock

    override def lock(): Unit = {
      logger.debug(s"Acquiring lock on $key")
      javaLock.lock()
      logger.debug(s"Acquired lock on $key")
    }

    override def tryLock(): Boolean = {
      val res = javaLock.tryLock()
      if (res) {
        logger.debug(s"Acquired lock on $key")
      }
      res
    }

    override def unlock(): Unit = {
      javaLock.unlock()
      logger.debug(s"Released lock on $key")
    }
  }

}