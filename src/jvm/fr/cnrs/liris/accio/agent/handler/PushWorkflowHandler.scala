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
import fr.cnrs.liris.accio.runtime.commandbus.AbstractHandler
import fr.cnrs.liris.accio.agent.{PushWorkflowRequest, PushWorkflowResponse}
import fr.cnrs.liris.accio.framework.api.thrift.{InvalidSpecException, InvalidSpecMessage}
import fr.cnrs.liris.accio.framework.service.WorkflowFactory
import fr.cnrs.liris.accio.framework.storage.Storage

import scala.collection.mutable

/**
 * Handle the creation of update of a workflow.
 *
 * @param workflowFactory Workflow factory.
 * @param storage         Storage.
 */
class PushWorkflowHandler @Inject()(workflowFactory: WorkflowFactory, storage: Storage)
  extends AbstractHandler[PushWorkflowRequest, PushWorkflowResponse] {

  @throws[InvalidSpecException]
  override def handle(req: PushWorkflowRequest): Future[PushWorkflowResponse] = {
    val warnings = mutable.Set.empty[InvalidSpecMessage]
    val workflow = workflowFactory.create(req.spec, req.user, warnings)
    storage.workflows.save(workflow)
    Future(PushWorkflowResponse(workflow, warnings.toSeq))
  }
}