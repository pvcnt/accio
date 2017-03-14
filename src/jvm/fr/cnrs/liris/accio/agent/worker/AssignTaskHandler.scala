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

package fr.cnrs.liris.accio.agent.worker

import com.google.inject.Inject
import com.twitter.util.Future
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.core.domain.InvalidTaskException

/**
 * Handle a request from the master, asking to execute a task on this worker node. It will launch an executor and
 * start monitoring it. It returns immediately, as soon as the request has been accepts.
 *
 * @param taskExecutor Task executor.
 * @param state        Worker state.
 */
final class AssignTaskHandler @Inject()(taskExecutor: WorkerTaskExecutor, state: WorkerState)
  extends Handler[AssignTaskRequest, AssignTaskResponse] {

  @throws[InvalidTaskException]
  override def handle(req: AssignTaskRequest): Future[AssignTaskResponse] = {
    state.register(req.task.id)
    taskExecutor.submit(req.task)
    Future.value(AssignTaskResponse())
  }
}