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

package fr.cnrs.liris.accio.core.service.handler

import com.google.inject.Inject
import com.twitter.util.Future
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.service.StateManager

class HeartbeatHandler @Inject()(stateManager: StateManager)
  extends Handler[HeartbeatRequest, HeartbeatResponse] with LazyLogging {

  override def handle(req: HeartbeatRequest): Future[HeartbeatResponse] = {
    val taskLock = stateManager.lock(s"task/${req.taskId.value}")
    taskLock.lock()
    try {
      stateManager.get(req.taskId) match {
        case None =>
          // If the task has already been removed, we do not want to recreate a new one. We do not throw an
          // exception because it can be a "normal" situation (if the thread sending heartbeat is stopping).
          logger.warn(s"Received heartbeat from unknown task: ${req.taskId.value}")
        case Some(task) =>
          val newTask = task.copy(state = task.state.copy(heartbeatAt = Some(System.currentTimeMillis())))
          stateManager.save(newTask)
          logger.debug(s"[T${req.taskId.value}] Received heartbeat")
      }
    } finally {
      taskLock.unlock()
    }
    Future(HeartbeatResponse())
  }
}