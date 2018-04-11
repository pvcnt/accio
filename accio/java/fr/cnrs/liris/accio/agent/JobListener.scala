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
import com.twitter.util.{Await, Future}
import fr.cnrs.liris.accio.api.JobCreatedEvent
import fr.cnrs.liris.accio.state.StateManager
import fr.cnrs.liris.accio.storage.{JobStore, Storage}

@Singleton
final class JobListener @Inject()(storage: Storage, stateManager: StateManager) {
  @Subscribe
  def onJobCreated(event: JobCreatedEvent): Unit = {
    Await.result(storage.jobs.get(event.name).flatMap {
      case None => Future.Done
      case Some(job) =>
        if (job.status.children.isDefined) {
          storage.jobs.list(JobStore.Query(parent = Some(job.name))).flatMap { resp =>
            Future.join(resp.results.map(child => stateManager.schedule(child, Some(job))))
          }
        } else {
          stateManager.schedule(job, None)
        }
    })
  }
}