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
      stateManager.get(req.taskId).foreach { task =>
        stateManager.remove(task.id)
        val runLock = stateManager.lock(s"run/${task.runId.value}")
        runLock.lock()
        try {
          runRepository.get(task.runId).foreach { run =>
            val newRun = if (req.result.exitCode == 0) {
              runManager.onSuccess(run, task.nodeName, req.result, Some(task.payload.cacheKey))
            } else {
              runManager.onFailed(run, task.nodeName, req.result)
            }
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
}