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
import fr.cnrs.liris.accio.core.domain.{RunRepository, UnknownTaskException}

class HeartbeatTaskHandler @Inject()(runRepository: RunRepository)
  extends Handler[HeartbeatTaskRequest, HeartbeatTaskResponse] {

  @throws[UnknownTaskException]
  override def handle(req: HeartbeatTaskRequest): Future[HeartbeatTaskResponse] = {
    runRepository.get(req.taskId) match {
      case None => throw new UnknownTaskException(req.taskId)
      case Some(task) =>
        val updatedTask = task.copy(state = task.state.copy(heartbeatAt = Some(System.currentTimeMillis())))
        runRepository.save(updatedTask)
        Future(HeartbeatTaskResponse())
    }
  }
}