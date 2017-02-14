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

package fr.cnrs.liris.accio.agent.handler

import com.google.inject.Inject
import com.twitter.util.Future
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.agent.{HeartbeatRequest, HeartbeatResponse}
import fr.cnrs.liris.accio.core.statemgr.StateManager
import fr.cnrs.liris.accio.core.storage.MutableTaskRepository

/**
 * Receive heartbeat for a task.
 *
 * @param stateManager   State manager.
 * @param taskRepository Task repository.
 */
class HeartbeatHandler @Inject()(stateManager: StateManager, taskRepository: MutableTaskRepository)
  extends Handler[HeartbeatRequest, HeartbeatResponse] with LazyLogging {

  override def handle(req: HeartbeatRequest): Future[HeartbeatResponse] = {
    val lock = stateManager.lock("write")
    lock.lock()
    try {
      taskRepository.get(req.taskId).foreach { task =>
        val newTask = task.copy(state = task.state.copy(heartbeatAt = Some(System.currentTimeMillis())))
        taskRepository.save(newTask)
        logger.debug(s"[T${req.taskId.value}] Received heartbeat")
      }
      Future(HeartbeatResponse())
    } finally {
      lock.unlock()
    }
  }
}