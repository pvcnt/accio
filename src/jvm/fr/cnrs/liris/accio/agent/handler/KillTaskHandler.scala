/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.runtime.commandbus.AbstractHandler
import fr.cnrs.liris.accio.framework.api.thrift.InvalidTaskException

/**
 * Handle a request from the master asking to kill a task, whose executor is managed by this worker.
 *
 * @param state        Worker state.
 * @param taskExecutor Task executor.
 */
final class KillTaskHandler @Inject()(state: WorkerState, taskExecutor: ExecutorTaskExecutor)
  extends AbstractHandler[KillTaskRequest, KillTaskResponse] {

  @throws[InvalidTaskException]
  override def handle(req: KillTaskRequest): Future[KillTaskResponse] = {
    // We want to unregister the task from the worker state AND send the request to the task executor, even in case
    // of an error. If we received the request it is supposed to be legitimate. If not, we do not want ot presume
    // if the request is really illegal or if the state/executor is in an invalid state.
    try {
      state.unregister(req.id)
    } finally {
      taskExecutor.kill(req.id)
    }
    Future.value(KillTaskResponse())
  }
}