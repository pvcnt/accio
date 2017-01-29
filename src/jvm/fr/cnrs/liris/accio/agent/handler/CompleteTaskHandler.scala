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
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent.{CompleteTaskRequest, CompleteTaskResponse, InvalidTaskException}
import fr.cnrs.liris.accio.core.domain.{OpResult, Run, Task}
import fr.cnrs.liris.accio.core.runtime.RunManager
import fr.cnrs.liris.accio.core.statemgr.{LockService, StateManager}
import fr.cnrs.liris.accio.core.storage.MutableRunRepository

/**
 * Handle the completion of a task. It will store the result and update state of the appropriate run.
 *
 * @param runRepository Run repository.
 * @param stateManager  State manager.
 * @param runManager    Run lifecycle manager.
 * @param lockService   Lock service.
 */
final class CompleteTaskHandler @Inject()(
  runRepository: MutableRunRepository,
  stateManager: StateManager,
  runManager: RunManager,
  lockService: LockService)
  extends Handler[CompleteTaskRequest, CompleteTaskResponse] with StrictLogging {

  @throws[InvalidTaskException]
  override def handle(req: CompleteTaskRequest): Future[CompleteTaskResponse] = {
    lockService.withLock(req.taskId) {
      stateManager.get(req.taskId) match {
        case None => throw new InvalidTaskException
        case Some(task) =>
          // Remove task from the state manager.
          stateManager.remove(task.id)

          // Update run (and maybe its parent).
          lockService.withLock(task.runId) {
            runRepository.get(task.runId) match {
              case None => throw new InvalidTaskException
              case Some(run) =>
                run.parent match {
                  case Some(parentId) =>
                    lockService.withLock(parentId) {
                      processRun(run, task, req.result, runRepository.get(parentId))
                    }
                  case None => processRun(run, task, req.result, None)
                }

            }
          }
      }
      Future(CompleteTaskResponse())
    }
  }

  private def processRun(run: Run, task: Task, result: OpResult, parent: Option[Run]) = {
    val (newRun, newParent) = if (result.exitCode == 0) {
      runManager.onSuccess(run, task.nodeName, result, Some(task.payload.cacheKey), parent)
    } else {
      runManager.onFailed(run, task.nodeName, result, parent)
    }
    runRepository.save(newRun)
    newParent.foreach(runRepository.save)
  }
}