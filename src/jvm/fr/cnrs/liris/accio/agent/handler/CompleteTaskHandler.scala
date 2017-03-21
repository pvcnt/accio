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
import fr.cnrs.liris.accio.agent.commandbus.AbstractHandler
import fr.cnrs.liris.accio.agent.{CompleteTaskRequest, CompleteTaskResponse}
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.framework.RunManager
import fr.cnrs.liris.accio.core.scheduler.{ClusterState, EventType, Scheduler}
import fr.cnrs.liris.accio.core.storage.Storage

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
    state.update(req.workerId, req.taskId, if (req.result.exitCode == 0) NodeStatus.Success else NodeStatus.Failed)
    storage.write { provider =>
      provider.runs.get(task.runId) match {
        case None => throw InvalidTaskException(task.id, Some(s"Task is associated with invalid run ${task.runId.value}"))
        case Some(run) =>
          run.parent match {
            case Some(parentId) => processRun(run, task, req.result, provider.runs.get(parentId))
            case None => processRun(run, task, req.result, None)
          }

      }
    }
    scheduler.houseKeeping(EventType.MoreResource)
    Future(CompleteTaskResponse())
  }

  private def processRun(run: Run, task: Task, result: OpResult, parent: Option[Run]) = {
    storage.write { provider =>
      val (newRun, newParent) = if (result.exitCode == 0) {
        runManager.onSuccess(run, task.nodeName, result, Some(task.payload.cacheKey), parent)
      } else {
        runManager.onFailed(run, task.nodeName, result, parent)
      }
      provider.runs.save(newRun)
      newParent.foreach(provider.runs.save)
    }
  }
}