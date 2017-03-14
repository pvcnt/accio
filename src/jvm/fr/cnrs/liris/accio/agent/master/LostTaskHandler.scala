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

package fr.cnrs.liris.accio.agent.master

import com.google.inject.Inject
import com.twitter.util.Future
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent.{InvalidWorkerException, LostTaskRequest, LostTaskResponse}
import fr.cnrs.liris.accio.core.domain.{InvalidTaskException, NodeStatus, Run, Task}
import fr.cnrs.liris.accio.core.framework.RunManager
import fr.cnrs.liris.accio.core.scheduler.ClusterState
import fr.cnrs.liris.accio.core.storage.MutableRunRepository

/**
 * @param runRepository Run repository.
 * @param runManager    Run lifecycle manager.
 * @param state         Cluster state.
 */
final class LostTaskHandler @Inject()(
  runRepository: MutableRunRepository,
  runManager: RunManager,
  state: ClusterState)
  extends Handler[LostTaskRequest, LostTaskResponse] with StrictLogging {

  @throws[InvalidTaskException]
  @throws[InvalidWorkerException]
  override def handle(req: LostTaskRequest): Future[LostTaskResponse] = {
    val worker = state.ensure(req.workerId, req.taskId)
    state.update(req.workerId, req.taskId, NodeStatus.Lost)
    val task = worker.runningTasks.find(_.id == req.taskId).get
    runRepository.get(task.runId).foreach { run =>
      run.parent match {
        case Some(parentId) => processRun(run, task, runRepository.get(parentId))
        case None => processRun(run, task, None)
      }
    }
    Future(LostTaskResponse())
  }

  private def processRun(run: Run, task: Task, parent: Option[Run]) = {
    val (newRun, newParent) = runManager.onLost(run, task.nodeName, parent)
    runRepository.save(newRun)
    newParent.foreach(runRepository.save)
  }
}