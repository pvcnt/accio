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

package fr.cnrs.liris.accio.tools.cli.controller

import com.twitter.util.{Future, Time}
import fr.cnrs.liris.accio.agent.{AgentService, GetWorkflowRequest}
import fr.cnrs.liris.accio.api.DataTypes
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.tools.cli.event.Reporter
import fr.cnrs.liris.util.StringUtils.padTo

class DescribeWorkflowController extends DescribeController[Workflow] with FormatHelper {
  private[this] val colWidth = 15

  override def retrieve(id: String, client: AgentService.MethodPerEndpoint): Future[Workflow] = {
    client
      .getWorkflow(GetWorkflowRequest(id))
      .map(_.workflow)
  }

  override def print(out: Reporter, workflow: Workflow): Unit = {
    out.outErr.printOutLn(s"${padTo("Id", colWidth)} ${workflow.id}")
    out.outErr.printOutLn(s"${padTo("Last version", colWidth)} ${workflow.version}")
    out.outErr.printOutLn(s"${padTo("Created", colWidth)} ${format(Time.fromMilliseconds(workflow.createdAt.get))}")
    out.outErr.printOutLn(s"${padTo("Owner", colWidth)} ${workflow.owner.map(_.name).getOrElse("<anonymous>")}")
    out.outErr.printOutLn(s"${padTo("Name", colWidth)} ${workflow.name}")
    out.outErr.printOutLn("Parameters")
    val maxLength = workflow.params.map(_.name.length).max
    workflow.params.foreach { argDef =>
      out.outErr.printOutLn(s"  ${padTo(argDef.name, maxLength)} ${argDef.help} (${DataTypes.stringify(argDef.kind)})")
    }
  }
}