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

package fr.cnrs.liris.accio.accioctl.controller

import com.twitter.util.{Future, Time}
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.core.domain.RunStatus

class GetRunController extends AbstractGetController[ListRunsResponse] with FormatHelper {
  override def retrieve(opts: GetQuery, client: AgentService$FinagleClient): Future[ListRunsResponse] = {
    val status = Set[RunStatus](RunStatus.Scheduled, RunStatus.Running) ++
      (if (opts.all) Set(RunStatus.Failed, RunStatus.Success, RunStatus.Killed) else Set.empty)
    val tags = if (opts.tags.nonEmpty) Some(opts.tags) else None
    val req = ListRunsRequest(owner = opts.owner, tags = tags, status = Some(status), limit = opts.limit)
    client.listRuns(req)
  }

  override protected def columns: Seq[(String, Int)] = Seq(
    ("id", 32),
    ("workflow", 15),
    ("created", 15),
    ("name", 30),
    ("status", 9))

  override protected def rows(resp: ListRunsResponse): Seq[Seq[Any]] = {
    resp.results.map { run =>
      val name = (if (run.children.nonEmpty) s"(${run.children.size}) " else "") + run.name.getOrElse("<no name>")
      Seq(
        run.id.value,
        run.pkg.workflowId.value,
        format(Time.fromMilliseconds(run.createdAt)),
        name,
        run.state.status.name)
    }
  }

  override protected def moreRows(resp: ListRunsResponse): Int = {
    if (resp.totalCount > resp.results.size) resp.totalCount - resp.results.size else 0
  }
}