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
import fr.cnrs.liris.accio.core.domain._

class CompletedTaskHandler @Inject()(runRepository: RunRepository)
  extends Handler[CompletedTaskRequest, CompletedTaskResponse] {

  @throws[UnknownTaskException]
  @throws[UnknownRunException]
  override def handle(req: CompletedTaskRequest): Future[CompletedTaskResponse] = {
    /*runRepository.get(req.taskId) match {
      case None => throw new UnknownTaskException(req.taskId)
      case Some(task) =>
        // Mark the task as completed.
        val now = System.currentTimeMillis()
        val taskStatus = if (req.result.exitCode == 0) TaskStatus.Success else TaskStatus.Failed
        val updatedTask = task.copy(state = task.state.copy(completedAt = Some(now), status = taskStatus))
        runRepository.save(updatedTask)

        runRepository.get(task.runId) match {
          case None => throw new UnknownRunException(task.runId)
          case Some(run) =>
            //TODO: deal with concurrency issues here.
            if (taskStatus == TaskStatus.Success) {
              val nodeState = run.state.nodes.find(_.nodeName == task.nodeName).get
              val updatedNodeState = nodeState.copy(completedAt = Some(System.currentTimeMillis()), result = Some(req.result))
              var updatedRunState = run.state.copy(nodes = run.state.nodes - nodeState + updatedNodeState)
              if (updatedRunState.nodes.forall(s => Utils.isCompleted(s.status))) {
                // If all nodes are completed, mark the whole run as completed.
                //if (updatedRunState.nodes.forall(_.status == NodeStatus))
              }
              runRepository.save(run.copy(state = updatedRunState))
            } else {
              // Cancel all dependant tasks.
            }
        }

        //TODO: schedule next nodes! or mark run as completed/failed/...
*/
    Future(CompletedTaskResponse())
  }
}