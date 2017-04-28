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
import fr.cnrs.liris.accio.agent.{LostTaskRequest, LostTaskResponse}
import fr.cnrs.liris.accio.framework.api.thrift._
import fr.cnrs.liris.accio.framework.scheduler.standalone.ClusterState
import fr.cnrs.liris.accio.framework.service.RunManager
import fr.cnrs.liris.accio.framework.storage.Storage
import fr.cnrs.liris.accio.runtime.commandbus.AbstractHandler

/**
 * @param storage    Storage.
 * @param runManager Run lifecycle manager.
 * @param state      Cluster state.
 */
final class LostTaskHandler @Inject()(storage: Storage, runManager: RunManager, state: ClusterState)
  extends AbstractHandler[LostTaskRequest, LostTaskResponse] with StrictLogging {

  @throws[InvalidTaskException]
  @throws[InvalidWorkerException]
  override def handle(req: LostTaskRequest): Future[LostTaskResponse] = {
    val worker = state.ensure(req.workerId, req.taskId)
    state.update(req.workerId, req.taskId, TaskState.Lost)
    val task = worker.activeTasks.find(_.id == req.taskId).get
    storage.runs.get(task.runId).foreach { run =>
      storage.runs.transactional(run.parent) { parent =>
        val (newRun, newParent) = runManager.onLost(run, task.nodeName, parent)
        storage.runs.save(newRun)
        newParent.foreach(storage.runs.save)
      }
    }
    Future(LostTaskResponse())
  }
}