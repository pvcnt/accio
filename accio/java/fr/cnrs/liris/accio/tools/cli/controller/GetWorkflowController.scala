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
import fr.cnrs.liris.accio.agent._

class GetWorkflowController extends AbstractGetController[ListWorkflowsResponse] with FormatHelper {
  override def retrieve(opts: GetQuery, client: AgentService$FinagleClient): Future[ListWorkflowsResponse] = {
    client.listWorkflows(ListWorkflowsRequest(owner = opts.owner, limit = opts.limit))
  }

  override protected def columns: Seq[(String, Int)] = Seq(
    ("id", 30),
    ("owner", 15),
    ("created", 15),
    ("name", 30))

  override protected def rows(resp: ListWorkflowsResponse): Seq[Seq[Any]] = {
    resp.workflows.map { workflow =>
      Seq(
        workflow.id,
        workflow.owner.map(_.name).getOrElse("<anonymous>"),
        format(Time.fromMilliseconds(workflow.createdAt.get)),
        workflow.name.getOrElse("<no name>"))
    }
  }

  override protected def moreRows(resp: ListWorkflowsResponse): Int = {
    if (resp.totalCount > resp.workflows.size) resp.totalCount - resp.workflows.size else 0
  }
}