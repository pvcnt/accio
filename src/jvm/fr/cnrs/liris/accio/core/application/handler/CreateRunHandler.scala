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
import fr.cnrs.liris.accio.core.application.SchedulerService
import fr.cnrs.liris.accio.core.domain._

class CreateRunHandler @Inject()(
  runFactory: RunFactory,
  runRepository: RunRepository,
  workflowRepository: WorkflowRepository,
  scheduler: SchedulerService,
  graphFactory: GraphFactory)
  extends Handler[CreateRunRequest, CreateRunResponse] {

  @throws[InvalidRunException]
  def handle(req: CreateRunRequest): Future[CreateRunResponse] = {
    // Create and save runs into the repository.
    val runs = runFactory.create(req.template, req.user)
    runs.foreach(runRepository.save)

    val maybeWorkflow = workflowRepository.get(runs.head.pkg.workflowId, runs.head.pkg.workflowVersion)
    maybeWorkflow match {
      case None => throw new UnknownWorkflowException(runs.head.pkg.workflowId, Some(runs.head.pkg.workflowVersion))
      case Some(workflow) =>
        // Schedule root nodes, for all runs (except the parent).
        val rootNodes = graphFactory.create(workflow.graph).roots
        runs.filter(_.children.isEmpty).foreach { run =>
          rootNodes.foreach { node =>
            scheduler.submit(run, node)
          }
        }
    }

    Future(CreateRunResponse(runs.map(_.id)))
  }
}