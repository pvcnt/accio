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
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.service.{RunLifecycleManager, StateManager}

final class CompleteTaskHandler @Inject()(
  runRepository: RunRepository,
  stateManager: StateManager,
  runManager: RunLifecycleManager)
  extends Handler[CompleteTaskRequest, CompleteTaskResponse] with StrictLogging {

  override def handle(req: CompleteTaskRequest): Future[CompleteTaskResponse] = {
    val taskLock = stateManager.lock(s"task/${req.taskId.value}")
    taskLock.lock()
    try {
      stateManager.get(req.taskId) match {
        case None => throw new UnknownTaskException(req.taskId)
        case Some(task) =>
          stateManager.remove(task.id)
          val runLock = stateManager.lock(s"run/${task.runId.value}")
          runLock.lock()
          try {
            runRepository.get(task.runId) match {
              case None => logger.warn(s"Received task ${req.taskId.value} for unknown run ${task.runId.value}")
              case Some(run) =>
                var newRun = if (req.result.exitCode == 0) {
                  runManager.onSuccess(run, task.nodeName, req.result, Some(task.payload.cacheKey))
                } else {
                  runManager.onFailed(run, task.nodeName, req.result)
                }
                newRun = newRun.copy(state = updateRunState(newRun.state))
                //TODO: update parent run if any.
                runRepository.save(newRun)
            }
          } finally {
            runLock.unlock()
          }
      }
      Future(CompleteTaskResponse())
    } finally {
      taskLock.unlock()
    }
  }

  private def updateRunState(runState: RunState) = {
    // Run could already be marked as completed if it was killed. In this case we do not want to update its state.
    // Otherwise, we check if this node was the last one to complete the run.
    if (runState.completedAt.isEmpty) {
      // If all nodes are completed (not necessarily successfully), mark the run as completed. It is successfully
      // completed if all nodes completed successfully.
      if (runState.nodes.forall(s => Utils.isCompleted(s.status))) {
        val newRunStatus = if (runState.nodes.forall(_.status == NodeStatus.Success)) {
          RunStatus.Success
        } else {
          RunStatus.Failed
        }
        runState.copy(progress = 1, status = newRunStatus, completedAt = Some(System.currentTimeMillis()))
      } else {
        // Run is not yet completed, only, update progress.
        val progress = runState.nodes.count(s => Utils.isCompleted(s.status)).toDouble / runState.nodes.size
        runState.copy(progress = progress)
      }
    } else {
      runState
    }
  }
}