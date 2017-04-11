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

package fr.cnrs.liris.accio.agent.handler.api

import com.google.inject.Inject
import com.twitter.util.Future
import fr.cnrs.liris.accio.runtime.commandbus.AbstractHandler
import fr.cnrs.liris.accio.agent.{ParseWorkflowRequest, ParseWorkflowResponse}
import fr.cnrs.liris.accio.core.api.{InvalidSpecException, InvalidSpecMessage}
import fr.cnrs.liris.accio.core.dsl.WorkflowParser

import scala.collection.mutable

/**
 * Parse a content written using workflow DSL into a workflow specification. It returns either a valid workflow
 * specification, possibly with some warnings, or a list of errors that the parser raised.
 *
 * @param workflowParser Workflow parser.
 */
final class ParseWorkflowHandler @Inject()(workflowParser: WorkflowParser)
  extends AbstractHandler[ParseWorkflowRequest, ParseWorkflowResponse] {

  override def handle(req: ParseWorkflowRequest): Future[ParseWorkflowResponse] = {
    val warnings = mutable.Set.empty[InvalidSpecMessage]
    try {
      val workflow = workflowParser.parse(req.content, req.filename, warnings)
      Future(ParseWorkflowResponse(workflow = Some(workflow), warnings = warnings.toSeq))
    } catch {
      case e: InvalidSpecException => Future(ParseWorkflowResponse(errors = e.errors, warnings = e.warnings))
    }
  }
}