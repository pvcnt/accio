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
import fr.cnrs.liris.accio.core.service.SchedulerService

/**
 * Handler for launching a workflow. It create one or several runs, save them and schedule them.
 *
 * @param runFactory         Run factory.
 * @param runRepository      Run repository.
 * @param workflowRepository Workflow repository.
 * @param scheduler          Scheduler service.
 * @param graphFactory       Graph factory.
 */
final class CreateRunHandler @Inject()(
  runFactory: RunFactory,
  runRepository: RunRepository,
  workflowRepository: WorkflowRepository,
  scheduler: SchedulerService,
  graphFactory: GraphFactory)
  extends Handler[CreateRunRequest, CreateRunResponse] with StrictLogging {

  @throws[InvalidRunDefException]
  def handle(req: CreateRunRequest): Future[CreateRunResponse] = {
    // Create runs.
    val runs = runFactory.create(req.defn, req.user)

    // Save only parent run.
    runs.filter(_.children.nonEmpty).foreach(runRepository.save)

    // Workflow does exist, because it has been validated when creating the runs.
    val workflow = workflowRepository.get(runs.head.pkg.workflowId, runs.head.pkg.workflowVersion).get
    val rootNodes = graphFactory.create(workflow.graph).roots

    // Schedule root nodes, for all child runs.
    runs.filter(_.children.isEmpty).foreach { run =>
      rootNodes.foreach { node =>
        runRepository.save(scheduler.submit(run, node))
      }
    }

    logger.debug(s"Created ${runs.size} runs, scheduled ${runs.size * rootNodes.size} nodes")
    Future(CreateRunResponse(runs.map(_.id)))
  }
}