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

package fr.cnrs.liris.accio.core.service

import fr.cnrs.liris.accio.core.domain.{Task, TaskId}

/**
 * State manager holds state about runs being executed and associated tasks. By opposition to
 * [[fr.cnrs.liris.accio.core.domain.RunRepository]], it should provide a faster and ephemeral (we can afford data
 * loss, even if it is not desired) storage, for small payloads. It also provides synchronization utils.
 *
 * State manager is a service which is highly solicited during run execution and highly concurrent.
 */
trait StateManager {
  /**
   * Return a distributed lock using this state manager. The lock is not yet acquired. Note that many [[Lock]]
   * instances sharing the same key are actually referring to the same lock object.
   *
   * @param key Locking key.
   */
  def lock(key: String): Lock

  def tasks: Set[Task]

  def save(task: Task): Unit

  def remove(id: TaskId): Unit

  def get(id: TaskId): Option[Task]
}

/**
 * Lock trait, provided by [[StateManager]]s. They are re-entrant, and implementations should stick to this.
 */
trait Lock {
  /**
   * Acquire this lock. This is a blocking operation.
   */
  def lock(): Unit

  /**
   * Try to acquire this lock. This is a non-blocking operation.
   *
   * @return True if the lock was acquired, false otherwise.
   */
  def tryLock(): Boolean

  /**
   * Release this lock.
   */
  def unlock(): Unit
}