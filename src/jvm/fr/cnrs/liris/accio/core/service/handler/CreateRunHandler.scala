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

package fr.cnrs.liris.accio.core.service.handler

import com.google.inject.Inject
import com.twitter.util.{Future, FuturePool}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.service.{RunLifecycleManager, StateManager}

/**
 * Handler for launching a workflow. It create one or several runs, save them and schedule them.
 *
 * @param runFactory    Run factory.
 * @param runRepository Run repository.
 * @param runManager    Run lifecycle manager.
 * @param stateManager  State manager.
 */
final class CreateRunHandler @Inject()(
  runFactory: RunFactory,
  runRepository: MutableRunRepository,
  runManager: RunLifecycleManager,
  stateManager: StateManager)
  extends AbstractHandler[CreateRunRequest, CreateRunResponse](stateManager) with StrictLogging {

  @throws[InvalidRunDefException]
  def handle(req: CreateRunRequest): Future[CreateRunResponse] = {
    val runs = runFactory.create(req.defn, req.user)
    runs.foreach(runRepository.save)

    // We save the runs before launching them asynchronously. We can return more quickly to the client, for a
    // better experience, as launching runs can take some times. Moreover, we must save runs first before launching
    // them, otherwise tasks referencing unknown runs could be running.
    FuturePool.unboundedPool {
      runs.foreach { run =>
        withLock(run.id) {
          runRepository.save(runManager.launch(run))
        }
      }
    }
    Future(CreateRunResponse(runs.map(_.id)))
  }
}