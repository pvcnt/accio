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
import fr.cnrs.liris.accio.agent.{UpdateRunRequest, UpdateRunResponse}
import fr.cnrs.liris.accio.framework.api.thrift.{Run, UnknownRunException}
import fr.cnrs.liris.accio.framework.storage.Storage
import fr.cnrs.liris.accio.runtime.commandbus.AbstractHandler

/**
 * Update metadata of a run.
 *
 * @param storage Storage.
 */
final class UpdateRunHandler @Inject()(storage: Storage)
  extends AbstractHandler[UpdateRunRequest, UpdateRunResponse] {

  @throws[UnknownRunException]
  override def handle(req: UpdateRunRequest): Future[UpdateRunResponse] = {
    storage.runs.transactional(req.id) {
      case None => throw UnknownRunException(req.id)
      case Some(run) =>
        run.parent match {
          case None => process(run, req)
          case Some(parentId) => storage.runs.foreach(parentId)(process(_, req))
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
    if (req.tags.nonEmpty) {
      newRun = newRun.copy(tags = req.tags)
    }
    storage.runs.save(newRun)
  }
}