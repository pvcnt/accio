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
import fr.cnrs.liris.accio.runtime.commandbus.AbstractHandler
import fr.cnrs.liris.accio.agent.{LostTaskRequest, LostTaskResponse}
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.accio.core.framework.RunManager
import fr.cnrs.liris.accio.core.scheduler.ClusterState
import fr.cnrs.liris.accio.core.storage.Storage

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
    state.update(req.workerId, req.taskId, NodeStatus.Lost)
    val task = worker.activeTasks.find(_.id == req.taskId).get
    storage.write { provider =>
      provider.runs.get(task.runId).foreach { run =>
        run.parent match {
          case Some(parentId) => processRun(run, task, provider.runs.get(parentId))
          case None => processRun(run, task, None)
        }
      }
    }
    Future(LostTaskResponse())
  }

  private def processRun(run: Run, task: Task, parent: Option[Run]) = {
    storage.write { provider =>
      val (newRun, newParent) = runManager.onLost(run, task.nodeName, parent)
      provider.runs.save(newRun)
      newParent.foreach(provider.runs.save)
    }
  }
}