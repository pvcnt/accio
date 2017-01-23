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
import com.twitter.util.Future
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.service.RunLifecycleManager

/**
 * Handler for launching a workflow. It create one or several runs, save them and schedule them.
 *
 * @param runFactory    Run factory.
 * @param runRepository Run repository.
 * @param runManager    Run lifecycle manager.
 */
final class CreateRunHandler @Inject()(
  runFactory: RunFactory,
  runRepository: RunRepository,
  runManager: RunLifecycleManager)
  extends Handler[CreateRunRequest, CreateRunResponse] with StrictLogging {

  @throws[InvalidRunDefException]
  def handle(req: CreateRunRequest): Future[CreateRunResponse] = {
    val runs = runFactory.create(req.defn, req.user)
    val scheduledRuns = runManager.launch(runs)
    scheduledRuns.foreach(runRepository.save)
    Future(CreateRunResponse(runs.map(_.id)))
  }
}