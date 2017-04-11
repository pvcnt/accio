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
import fr.cnrs.liris.accio.core.api.thrift.{InvalidExecutorException, InvalidTaskException, InvalidWorkerException}

/**
 * Handle a request from an executor containing execution logs, which will be forwarded to the master.
 *
 * @param client Client for the master server.
 * @param state  Worker state.
 */
final class StreamExecutorLogsHandler @Inject()(client: AgentService$FinagleClient, state: WorkerState)
  extends AbstractHandler[StreamExecutorLogsRequest, StreamExecutorLogsResponse] with LazyLogging {

  @throws[InvalidTaskException]
  @throws[InvalidExecutorException]
  override def handle(req: StreamExecutorLogsRequest): Future[StreamExecutorLogsResponse] = {
    state.ensure(req.taskId, req.executorId)
    client
      .streamTaskLogs(StreamTaskLogsRequest(state.workerId, req.taskId, req.logs))
      .rescue {
        case e: InvalidWorkerException =>
          logger.error("Invalid worker state", e)
          Future.const(Throw(InvalidExecutorException(req.executorId)))
      }
      .map(_ => StreamExecutorLogsResponse())
  }
}