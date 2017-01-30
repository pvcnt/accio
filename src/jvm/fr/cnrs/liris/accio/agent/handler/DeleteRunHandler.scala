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
import fr.cnrs.liris.accio.agent.{DeleteRunRequest, DeleteRunResponse}
import fr.cnrs.liris.accio.core.statemgr.LockService
import fr.cnrs.liris.accio.core.storage.{MutableRunRepository, RunQuery}

/**
 * Delete a run, and all its children if it is a parent run.
 *
 * @param runRepository Run repository.
 * @param lockService   Lock service.
 */
final class DeleteRunHandler @Inject()(runRepository: MutableRunRepository, lockService: LockService)
  extends Handler[DeleteRunRequest, DeleteRunResponse] {

  override def handle(req: DeleteRunRequest): Future[DeleteRunResponse] = {
    lockService.withLock(req.id) {
      runRepository.get(req.id).foreach { run =>
        if (run.children > 0) {
          // Delete child runs, if any.
          val children = runRepository.find(RunQuery(parent = Some(run.id))).results
          children.foreach { child =>
            lockService.withLock(child.id) {
              runRepository.remove(child.id)
            }
          }
        } else if (run.parent.isDefined) {
          // Update parent run, if any.
          lockService.withLock(run.parent.get) {
            runRepository.get(run.parent.get).foreach { parent =>
              if (parent.children > 1) {
                runRepository.save(parent.copy(children = parent.children - 1))
              } else {
                runRepository.remove(parent.id)
              }
            }
          }
        }
        runRepository.remove(run.id)
      }
    }
    Future(DeleteRunResponse())
  }
}