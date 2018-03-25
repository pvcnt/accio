/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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
import fr.cnrs.liris.accio.agent.{CompleteTaskRequest, CompleteTaskResponse}
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.scheduler.standalone.ClusterState
import fr.cnrs.liris.accio.scheduler.{EventType, Scheduler}
import fr.cnrs.liris.accio.service.RunManager
import fr.cnrs.liris.accio.storage.Storage
import fr.cnrs.liris.accio.runtime.commandbus.AbstractHandler

/**
 * Handle the completion of a task. It will store the result and update state of the appropriate run.
 *
 * @param storage    Storage.
 * @param runManager Run lifecycle manager.
 * @param scheduler  Scheduler.
 * @param state      Cluster state.
 */
final class CompleteTaskHandler @Inject()(
  storage: Storage,
  runManager: RunManager,
  scheduler: Scheduler,
  state: ClusterState)
  extends AbstractHandler[CompleteTaskRequest, CompleteTaskResponse] with StrictLogging {

  @throws[InvalidTaskException]
  @throws[InvalidWorkerException]
  override def handle(req: CompleteTaskRequest): Future[CompleteTaskResponse] = {
    val worker = state.ensure(req.workerId, req.taskId)
    val task = worker.activeTasks.find(_.id == req.taskId).get
    state.update(req.workerId, req.taskId, if (req.result.exitCode == 0) TaskState.Success else TaskState.Failed)
    storage.runs.transactional(task.runId) {
      case None => throw InvalidTaskException(task.id, Some(s"Task is associated with invalid run ${task.runId.value}"))
      case Some(run) =>
        storage.runs.transactional(run.parent) { parent =>
          val (newRun, newParent) =
            if (req.result.exitCode == 0) {
              runManager.onSuccess(run, task.nodeName, req.result, Some(task.payload.cacheKey), parent)
            } else {
              runManager.onFailed(run, task.nodeName, req.result, parent)
            }
          storage.runs.save(newRun)
          newParent.foreach(storage.runs.save)
        }
    }
    scheduler.houseKeeping(EventType.MoreResource)
    Future(CompleteTaskResponse())
  }
}