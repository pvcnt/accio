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
import fr.cnrs.liris.accio.server._
import fr.cnrs.liris.accio.api.thrift.ExecState

class GetJobController extends AbstractGetController[ListJobsResponse] with FormatHelper {
  override def retrieve(opts: GetQuery, client: AgentService.MethodPerEndpoint): Future[ListJobsResponse] = {
    val states = Set[ExecState](ExecState.Pending, ExecState.Scheduled, ExecState.Running) ++
      (if (opts.all) Set(ExecState.Failed, ExecState.Successful, ExecState.Killed) else Set.empty)
    val req = ListJobsRequest(
      author = opts.author,
      tags = if (opts.tags.nonEmpty) Some(opts.tags) else None,
      state = Some(states),
      limit = opts.limit)
    client.listJobs(req)
  }

  override protected def columns: Seq[(String, Int)] = Seq(
    ("name", 32),
    ("created", 15),
    ("title", 30),
    ("status", 9))

  override protected def rows(resp: ListJobsResponse): Seq[Seq[Any]] = {
    resp.jobs.map { job =>
      val name = (if (job.status.children.isDefined) s"(${job.status.children.get.values.sum}) " else "") +
        job.title.getOrElse("<untitled>")
      Seq(
        job.name,
        format(Time.fromMilliseconds(job.createTime)),
        name,
        job.status.state.name)
    }
  }

  override protected def moreRows(resp: ListJobsResponse): Int = {
    if (resp.totalCount > resp.jobs.size) resp.totalCount - resp.jobs.size else 0
  }
}