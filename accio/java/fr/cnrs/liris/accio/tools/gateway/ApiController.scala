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

package fr.cnrs.liris.accio.tools.gateway

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.finatra.validation.{Max, Min}
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.util.StringUtils.explode

@Singleton
final class ApiController @Inject()(client: AgentService.MethodPerEndpoint) extends Controller {
  get("/api/v1") { _: Request =>
    client.getCluster(GetClusterRequest()).map { resp =>
      IndexResponse(resp.version)
    }
  }

  get("/api/v1/operator") { httpReq: ListOperatorsHttpRequest =>
    val req = ListOperatorsRequest(httpReq.includeDeprecated)
    client
      .listOperators(req)
      .map(resp => ListOperatorsHttpResponse(resp.operators))
  }

  get("/api/v1/operator/:name") { httpReq: GetOperatorHttpRequest =>
    client
      .getOperator(GetOperatorRequest(httpReq.name))
      .map(_.operator)
  }

  get("/api/v1/job") { httpReq: ListJobsHttpRequest =>
    val offset = (httpReq.page - 1) * httpReq.perPage
    val req = ListJobsRequest(
      title = httpReq.title,
      author = httpReq.author,
      parent = httpReq.parent,
      clonedFrom = httpReq.clonedFrom,
      state = httpReq.state.map(v => ExecState.valueOf(v).toSet),
      tags = if (httpReq.tags.nonEmpty) Some(explode(httpReq.tags, ",")) else None,
      q = httpReq.q,
      limit = Some(httpReq.perPage),
      offset = Some(offset))
    client
      .listJobs(req)
      .map(resp => ListJobsHttpResponse(resp.jobs, resp.totalCount))
  }

  get("/api/v1/job/:name") { httpReq: GetJobHttpRequest =>
    client
      .getJob(GetJobRequest(httpReq.name))
      .map { resp =>
        val job = resp.job.copy(steps = Seq.empty)
        if (httpReq.download) {
          response
            .ok(job)
            .header("Content-Disposition", s"attachment; filename=job-${job.name}.json")
        } else {
          job
        }
      }
  }

  get("/api/v1/job/:name/logs/:task/:kind") { httpReq: ListLogsHttpRequest =>
    val req = ListLogsRequest(
      job = httpReq.name,
      step = httpReq.task,
      kind = httpReq.kind,
      skip = httpReq.skip)
    client
      .listLogs(req)
      .map { resp =>
        if (httpReq.download) {
          response
            .ok(resp.results.mkString("\n"))
            .header("Content-Disposition", s"attachment; filename=job-${httpReq.name}_${httpReq.task}_${httpReq.kind}.txt")
        } else {
          resp.results
        }
      }
  }
}

case class IndexResponse(version: String)

case class ListOperatorsHttpRequest(@QueryParam includeDeprecated: Boolean = false)

case class ListOperatorsHttpResponse(operators: Seq[Operator])

case class GetOperatorHttpRequest(@RouteParam name: String)

case class ListJobsHttpRequest(
  @QueryParam author: Option[String],
  @QueryParam title: Option[String],
  @QueryParam state: Option[String],
  @QueryParam parent: Option[String],
  @QueryParam clonedFrom: Option[String],
  @QueryParam tags: Option[String],
  @QueryParam q: Option[String],
  @QueryParam @Min(1) page: Int = 1,
  @QueryParam @Min(0) @Max(50) perPage: Int = 15)

case class ListJobsHttpResponse(jobs: Seq[Job], totalCount: Int)

case class GetJobHttpRequest(@RouteParam name: String, @QueryParam download: Boolean = false)

case class ListLogsHttpRequest(
  @RouteParam name: String,
  @RouteParam task: String,
  @RouteParam kind: String,
  @QueryParam @Min(0) skip: Option[Int],
  @QueryParam download: Boolean = false)

