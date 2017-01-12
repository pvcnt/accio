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

class RegisterExecutorHandler @Inject()(runRepository: RunRepository)
  extends Handler[RegisterExecutorRequest, RegisterExecutorResponse] {

  @throws[UnknownTaskException]
  @throws[UnknownRunException]
  override def handle(req: RegisterExecutorRequest): Future[RegisterExecutorResponse] = {
    /*runRepository.get(req.taskId) match {
      case None => throw new UnknownTaskException(req.taskId)
      case Some(task) =>
        runRepository.get(task.runId) match {
          case None => throw new UnknownRunException(task.runId)
          case Some(run) =>
            //TODO: deal with concurrency issues here.
            val now = System.currentTimeMillis()
            // Mark the task as started.
            val updatedTask = task.copy(state = task.state.copy(startedAt = Some(now), status = TaskStatus.Running))
            runRepository.save(updatedTask)

            val nodeState = run.state.nodes.find(_.nodeName == task.nodeName).get
            if (nodeState.startedAt.isEmpty) {
              // Only in case the node is not marked as started. It can already be marked as started if another task
              // was already spawned for this node.
              val updatedNodeState = nodeState.copy(startedAt = Some(now), status = NodeStatus.Running)
              var updatedRunState = run.state.copy(nodes = run.state.nodes - nodeState + updatedNodeState)
              if (run.state.startedAt.isEmpty) {
                // If the run is not already marked as started, do it now.
                updatedRunState = updatedRunState.copy(startedAt = Some(now))
              }
              runRepository.save(run.copy(state = updatedRunState))
            }
        }
        Future(RegisterExecutorResponse(task.payload))
    }*/
    ???
  }
}