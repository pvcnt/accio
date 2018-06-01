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

package fr.cnrs.liris.lumos.gateway

import com.google.inject.{Inject, Singleton}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.exceptions.BadRequestException
import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.finatra.validation.{Max, Min}
import com.twitter.util.Future
import fr.cnrs.liris.lumos.domain.thrift
import fr.cnrs.liris.lumos.server._

@Singleton
final class ApiController @Inject()(client: LumosService.MethodPerEndpoint) extends Controller {
  get("/api/v1/jobs") { httpReq: ListJobsHttpRequest =>
    val maybeState = httpReq.state.map(thrift.ExecState.valueOf)
    if (maybeState.exists(_.isEmpty)) {
      Future.exception(BadRequestException(s"Invalid state: ${httpReq.state.get}"))
    } else {
      val offset = (httpReq.page - 1) * httpReq.perPage
      val req = ListJobsRequest(
        owner = httpReq.owner,
        state = maybeState.map(v => Set(v.get)),
        labels = httpReq.labels,
        limit = Some(httpReq.perPage),
        offset = Some(offset))
      client
        .listJobs(req)
        .map(resp => ListJobsHttpResponse(resp.jobs.map(_.unsetTasks), resp.totalCount))
    }
  }

  get("/api/v1/jobs/:name") { httpReq: GetJobHttpRequest =>
    client
      .getJob(GetJobRequest(httpReq.name))
      .map { resp =>
        if (httpReq.download) {
          response.ok(resp.job)
            .header("Content-Disposition", s"attachment; filename=job-${resp.job.name}.json")
        } else {
          resp.job
        }
      }
  }
}

case class ListJobsHttpRequest(
  @QueryParam owner: Option[String],
  @QueryParam state: Option[String],
  @QueryParam labels: Option[String],
  @QueryParam @Min(1) page: Int = 1,
  @QueryParam @Min(0) @Max(50) perPage: Int = 15)

case class ListJobsHttpResponse(jobs: Seq[thrift.Job], totalCount: Long)

case class GetJobHttpRequest(@RouteParam name: String, @QueryParam download: Boolean = false)