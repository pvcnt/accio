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
import fr.cnrs.liris.accio.agent.{UnknownRunException, UpdateRunRequest, UpdateRunResponse}
import fr.cnrs.liris.accio.core.domain.Run
import fr.cnrs.liris.accio.core.statemgr.LockService
import fr.cnrs.liris.accio.core.storage.MutableRunRepository

/**
 * Update metadata of a run.
 *
 * @param runRepository Run repository.
 * @param lockService   Lock service.
 */
final class UpdateRunHandler @Inject()(runRepository: MutableRunRepository, lockService: LockService)
  extends Handler[UpdateRunRequest, UpdateRunResponse] {

  override def handle(req: UpdateRunRequest): Future[UpdateRunResponse] = {
    lockService.withLock(req.id) {
      runRepository.get(req.id) match {
        case None => throw new UnknownRunException()
        case Some(run) =>
          run.parent match {
            case None => process(run, req)
            case Some(parentId) =>
              lockService.withLock(parentId) {
                runRepository.get(parentId).foreach(process(_, req))
              }
          }
      }
    }
    Future(UpdateRunResponse())
  }

  private def process(run: Run, req: UpdateRunRequest) = {
    var newRun = run
    req.name.foreach { name =>
      newRun = newRun.copy(name = Some(name))
    }
    req.notes.foreach { notes =>
      newRun = newRun.copy(notes = Some(notes))
    }
    req.tags.foreach { tags =>
      newRun = newRun.copy(tags = tags)
    }
    runRepository.save(newRun)
  }
}