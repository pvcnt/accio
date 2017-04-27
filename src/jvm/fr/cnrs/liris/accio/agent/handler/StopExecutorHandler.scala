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
import com.twitter.util.{Future, Throw}
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.runtime.commandbus.AbstractHandler
import fr.cnrs.liris.accio.framework.api.thrift.{InvalidExecutorException, InvalidTaskException, InvalidWorkerException}

/**
 * Handle a request from an executor who completed the task that was assigned to it (either successfully or not).
 * From that moment, we do not expect the executor to send further heartbeats. The master will be signaled that the
 * task has completed.
 *
 * @param client Client for the master server.
 * @param state  Worker state.
 */
final class StopExecutorHandler @Inject()(client: AgentService$FinagleClient, state: WorkerState)
  extends AbstractHandler[StopExecutorRequest, StopExecutorResponse] with LazyLogging {

  @throws[InvalidTaskException]
  @throws[InvalidExecutorException]
  override def handle(req: StopExecutorRequest): Future[StopExecutorResponse] = {
    state.unassign(req.executorId, req.taskId)
    client
      .completeTask(CompleteTaskRequest(state.workerId, req.taskId, req.result))
      .rescue {
        case e: InvalidWorkerException =>
          logger.error("Invalid worker state", e)
          Future.const(Throw(InvalidExecutorException(req.executorId)))
      }
      .map(_ => StopExecutorResponse())
  }
}