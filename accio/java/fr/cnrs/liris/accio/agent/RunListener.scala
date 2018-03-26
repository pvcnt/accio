/*
 * Accio is a platform to launch computer science experiments.
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

package fr.cnrs.liris.accio.agent

import com.google.common.eventbus.Subscribe
import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.api.RunCreatedEvent
import fr.cnrs.liris.accio.service.RunManager
import fr.cnrs.liris.accio.storage.Storage

@Singleton
final class RunListener @Inject()(storage: Storage, runManager: RunManager) {
  @Subscribe
  def onRunCreated(event: RunCreatedEvent): Unit = {
    // TODO: we still have a too long lock here. If we have thousand child runs, we could hold the
    // lock for too much time.
    storage.runs.foreach(event.runId) { parent =>
      // We are guaranteed there will be at least one run. It is either a single run or the parent
      // run, we may launch it safely.
      var (newParent, _) = runManager.launch(parent, None)

      // Sequentially launch the children runs.
      event.children.foreach { childId =>
        storage.runs.foreach(childId) { run =>
          val res = runManager.launch(run, Some(newParent))
          storage.runs.save(res._1)
          res._2.foreach { nextParent =>
            newParent = nextParent
          }
        }
      }
      // We finally save the parent run.
      // TODO: maybe we should save it after each child run started.
      storage.runs.save(newParent)
    }
  }
}
