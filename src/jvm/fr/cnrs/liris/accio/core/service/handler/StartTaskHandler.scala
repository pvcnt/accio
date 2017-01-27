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
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.service.{RunLifecycleManager, StateManager}

class StartTaskHandler @Inject()(runRepository: MutableRunRepository, stateManager: StateManager, runManager: RunLifecycleManager)
  extends AbstractHandler[StartTaskRequest, StartTaskResponse](stateManager) {

  @throws[UnknownTaskException]
  @throws[UnknownRunException]
  override def handle(req: StartTaskRequest): Future[StartTaskResponse] = {
    // Concurrency: We do not need to lock around the task because there should not be any lock yet. Until a payload
    // has been received by the executor, there should not be any heartbeat or task completed request.
    stateManager.get(req.taskId) match {
      case None => throw new UnknownTaskException(req.taskId)
      case Some(task) =>
        withLock(task.runId) {
          runRepository.get(task.runId) match {
            case None =>
              // Illegal state: no run found for this task.
              stateManager.remove(req.taskId)
              throw new UnknownRunException(task.runId)
            case Some(run) =>
              // Update task.
              val now = System.currentTimeMillis()
              val newTask = task.copy(state = task.state.copy(startedAt = Some(now), heartbeatAt = Some(now), status = TaskStatus.Running))
              stateManager.save(newTask)

              // Update run.
              val newRun = runManager.onStart(run, task.nodeName)
              runRepository.save(newRun)

              // Transmit payload to the executor.
              Future(StartTaskResponse(task.runId, task.nodeName, task.payload))
          }
        }
    }
  }
}