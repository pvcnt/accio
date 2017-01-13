/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.application.handler

import com.google.inject.Inject
import com.twitter.util.Future
import fr.cnrs.liris.accio.core.application.StateManager
import fr.cnrs.liris.accio.core.domain._

class StartTaskHandler @Inject()(runRepository: RunRepository, stateManager: StateManager)
  extends Handler[StartTaskRequest, StartTaskResponse] {

  @throws[UnknownTaskException]
  @throws[UnknownRunException]
  override def handle(req: StartTaskRequest): Future[StartTaskResponse] = {
    // Concurrency: We do not need to lock around the task because there should not be any lock yet. Until a payload
    // has been received by the executor, there should not be any heartbeat or task completed request.
    stateManager.get(req.taskId) match {
      case None => throw new UnknownTaskException(req.taskId)
      case Some(task) =>
        val runLock = stateManager.createLock(s"run/${task.runId.value}")
        runLock.lock()
        try {
          runRepository.get(task.runId) match {
            case None =>
              // Illegal state: no run found for this task.
              stateManager.remove(req.taskId)
              throw new UnknownRunException(task.runId)
            case Some(run) =>
              val now = System.currentTimeMillis()
              updateRun(run, task.nodeName, now)
              updateTask(task, now)
              Future(StartTaskResponse(task.runId, task.nodeName, task.payload))
          }
        } finally {
          runLock.unlock()
        }
    }
  }

  private def updateRun(run: Run, nodeName: String, now: Long) = {
    val nodeState = run.state.nodes.find(_.nodeName == nodeName).get
    // Node state could be already marked as started if another task was already spawned for this node.
    if (nodeState.startedAt.isEmpty) {
      // Mark node as started, and run as started if not already.
      val newNodeState = nodeState.copy(startedAt = Some(now), status = NodeStatus.Running)
      var newRunState = run.state.copy(nodes = run.state.nodes - nodeState + newNodeState)
      if (run.state.startedAt.isEmpty) {
        newRunState = newRunState.copy(startedAt = Some(now))
      }
      runRepository.save(run.copy(state = newRunState))
    }
  }

  private def updateTask(task:Task, now: Long) = {
    // Mark task as started.
    val newTask = task.copy(state = task.state.copy(startedAt = Some(now), status = TaskStatus.Running))
    stateManager.save(newTask)
  }
}