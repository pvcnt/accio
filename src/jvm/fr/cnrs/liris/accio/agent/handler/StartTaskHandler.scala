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
import fr.cnrs.liris.accio.agent.{InvalidTaskException, StartTaskRequest, StartTaskResponse}
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.runtime.RunManager
import fr.cnrs.liris.accio.core.statemgr.StateManager
import fr.cnrs.liris.accio.core.storage.{MutableRunRepository, MutableTaskRepository}

/**
 * When the executor starts its execution, it only has the identifier of a task to execute. Its first action should
 * be to register itself to the agent and ask for a payload to execute. This is what is done here.
 *
 * @param runRepository  Run repository.
 * @param taskRepository Task repository.
 * @param stateManager   State manager.
 * @param runManager     Run lifecycle manager.
 */
class StartTaskHandler @Inject()(
  runRepository: MutableRunRepository,
  taskRepository: MutableTaskRepository,
  stateManager: StateManager,
  runManager: RunManager)
  extends Handler[StartTaskRequest, StartTaskResponse] {

  @throws[InvalidTaskException]
  override def handle(req: StartTaskRequest): Future[StartTaskResponse] = {
    val lock = stateManager.lock("write")
    lock.lock()
    try {
      taskRepository.get(req.taskId) match {
        case None => throw new InvalidTaskException
        case Some(task) =>
          runRepository.get(task.runId) match {
            case None =>
              // Illegal state: no run found for this task.
              taskRepository.remove(req.taskId)
              throw new InvalidTaskException
            case Some(run) =>
              // Update task. Mark a first heartbeat now to prevent the task to be marked as lost before starting.
              val now = System.currentTimeMillis()
              val newTask = task.copy(state = task.state.copy(startedAt = Some(now), heartbeatAt = Some(now), status = TaskStatus.Running))
              taskRepository.save(newTask)

              // Update run.
              val newRun = runManager.onStart(run, task.nodeName)
              runRepository.save(newRun)

              Future(StartTaskResponse(task.runId, task.nodeName, task.payload))
          }
      }
    } finally {
      lock.unlock()
    }
  }
}