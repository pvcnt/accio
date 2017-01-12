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

package fr.cnrs.liris.accio.executor

import com.google.inject.Inject
import com.twitter.util.{Future, Return, Throw}
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.application.handler.RegisterExecutorRequest
import fr.cnrs.liris.accio.core.domain.TaskId
import fr.cnrs.liris.accio.thrift.agent.TaskTrackerService

class ExecutorController @Inject()(trackerClient: TaskTrackerService.FinagledClient, taskExecutor: TaskExecutor) extends LazyLogging {
  def execute(opts: AccioExecutorFlags): Future[Unit] = {
    trackerClient
      .register(RegisterExecutorRequest(TaskId(opts.taskId)))
      .transform {
        case Throw(e) =>
          logger.error("Error while registering executor", e)
          Future.Done
        case Return(resp) => taskExecutor.submit(TaskId(opts.taskId), resp.runId, resp.nodeName, resp.payload)
      }
  }
}