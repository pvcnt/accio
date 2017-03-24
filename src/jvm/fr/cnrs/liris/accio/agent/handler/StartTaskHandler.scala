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
import fr.cnrs.liris.accio.agent.commandbus.AbstractHandler
import fr.cnrs.liris.accio.agent.{StartTaskRequest, StartTaskResponse}
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.framework.RunManager
import fr.cnrs.liris.accio.core.scheduler.ClusterState
import fr.cnrs.liris.accio.core.storage.Storage

/**
 * When the executor starts its execution, it only has the identifier of a task to execute. Its first action should
 * be to register itself to the agent and ask for a payload to execute. This is what is done here.
 *
 * @param storage    Storage.
 * @param runManager Run lifecycle manager.
 * @param state      Cluster state.
 */
class StartTaskHandler @Inject()(storage: Storage, runManager: RunManager, state: ClusterState)
  extends AbstractHandler[StartTaskRequest, StartTaskResponse] with LazyLogging {

  @throws[InvalidTaskException]
  @throws[InvalidWorkerException]
  override def handle(req: StartTaskRequest): Future[StartTaskResponse] = {
    val worker = state.ensure(req.workerId, req.taskId)
    val task = worker.activeTasks.find(_.id == req.taskId).get
    storage.write { provider =>
      provider.runs.get(task.runId) match {
        case None =>
          // Illegal state: no run found for this task.
          logger.warn(s"Task ${task.id.value} is associated with unknown run ${task.runId.value}")
          throw InvalidTaskException(task.id, Some(s"Task is associated with invalid run ${task.runId.value}"))
        case Some(run) =>
          state.update(worker.id, task.id, NodeStatus.Running)
          val newRun = runManager.onStart(run, task.nodeName)
          provider.runs.save(newRun)
          Future(StartTaskResponse(task.runId, task.nodeName, task.payload))
      }
    }
  }
}