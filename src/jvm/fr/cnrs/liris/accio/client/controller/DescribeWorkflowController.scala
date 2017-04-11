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

package fr.cnrs.liris.accio.client.controller

import com.twitter.util.{Future, Time}
import fr.cnrs.liris.accio.agent.{AgentService$FinagleClient, GetWorkflowRequest}
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.accio.runtime.event.Reporter
import fr.cnrs.liris.dal.core.api.DataTypes
import fr.cnrs.liris.common.util.StringUtils.padTo

class DescribeWorkflowController extends DescribeController[Workflow] with FormatHelper {
  private[this] val colWidth = 15

  override def retrieve(id: String, client: AgentService$FinagleClient): Future[Workflow] = {
    client.getWorkflow(GetWorkflowRequest(WorkflowId(id)))
      .map { resp =>
        resp.result match {
          case None => throw new NoResultException
          case Some(workflow) => workflow
        }
      }
  }

  override def print(out: Reporter, workflow: Workflow): Unit = {
    out.outErr.printOutLn(s"${padTo("Id", colWidth)} ${workflow.id.value}")
    out.outErr.printOutLn(s"${padTo("Last version", colWidth)} ${workflow.version}")
    out.outErr.printOutLn(s"${padTo("Created", colWidth)} ${format(Time.fromMilliseconds(workflow.createdAt.get))}")
    out.outErr.printOutLn(s"${padTo("Owner", colWidth)} ${workflow.owner.map(_.name).getOrElse("<anonymous>")}")
    out.outErr.printOutLn(s"${padTo("Name", colWidth)} ${workflow.name}")
    out.outErr.printOutLn("Parameters")
    val maxLength = workflow.params.map(_.name.length).max
    workflow.params.foreach { argDef =>
      out.outErr.printOutLn(s"  ${padTo(argDef.name, maxLength)} ${argDef.help} (${DataTypes.toString(argDef.kind)})")
    }
  }
}