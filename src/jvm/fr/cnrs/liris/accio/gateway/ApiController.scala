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

package fr.cnrs.liris.accio.gateway

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.finatra.validation.{Max, Min}
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.common.util.StringUtils.explode
import org.joda.time.DateTime

@Singleton
class ApiController @Inject()(client: AgentService.FinagledClient) extends Controller {
  get("/api/v1/workflow") { httpReq: ListWorkflowsHttpRequest =>
    val offset = (httpReq.page - 1) * httpReq.perPage
    val req = ListWorkflowsRequest(
      name = httpReq.name,
      owner = httpReq.owner,
      limit = Some(httpReq.perPage),
      offset = Some(offset))

    client.listWorkflows(req).map { resp =>
      ResultListResponse(resp.results, resp.totalCount)
    }
  }

  post("/api/v1/workflow") { req: Request =>
    response.badRequest
  }

  get("/api/v1/workflow/:id") { httpReq: GetWorkflowHttpRequest =>
    val req = GetWorkflowRequest(WorkflowId(httpReq.id), httpReq.version)
    client.getWorkflow(req).map(_.result).map {
      case Some(workflow) =>
        if (httpReq.download) {
          response.ok(workflow).header("Content-Disposition", s"attachment; filename=${workflow.id.value}.json")
        } else {
          workflow
        }
      case None => response.notFound
    }
  }

  get("/api/v1/run") { httpReq: ListRunsHttpRequest =>
    val offset = (httpReq.page - 1) * httpReq.perPage
    val req = ListRunsRequest(
      name = httpReq.name,
      owner = httpReq.owner,
      workflowId = httpReq.workflow.map(WorkflowId.apply),
      parent = httpReq.parent.map(RunId.apply),
      clonedFrom = httpReq.clonedFrom.map(RunId.apply),
      status = httpReq.status.map(v => RunStatus.valueOf(v).toSet),
      tags = httpReq.tags.map(explode(_, ",")),
      limit = Some(httpReq.perPage),
      offset = Some(offset))

    client.listRuns(req).map { resp =>
      ResultListResponse(resp.results, resp.totalCount)
    }
  }

  post("/api/v1/run") { req: Request =>
    response.badRequest
  }

  get("/api/v1/run/:id") { httpReq: GetRunHttpRequest =>
    val req = GetRunRequest(RunId(httpReq.id))
    client.getRun(req).map(_.result).map {
      case Some(run) =>
        if (httpReq.download) {
          // We download the entirety of the run, including artifacts and metrics.
          response
            .ok(run)
            .header("Content-Disposition", s"attachment; filename=run-${run.id.value}.json")
        } else {
          // In other cases, we do not transmit node result.
          run.copy(state = run.state.copy(nodes = run.state.nodes.map(_.unsetResult)))
        }
      case None => response.notFound
    }
  }

  get("/api/v1/run/:id/artifacts/:node") { httpReq: ListArtifactsHttpRequest =>
    val req = GetRunRequest(RunId(httpReq.id))
    client.getRun(req).map(_.result).map {
      case Some(run) => run.state.nodes.find(_.name == httpReq.node).flatMap(_.result).map(_.artifacts)
      case None => response.notFound
    }
  }

  post("/api/v1/run/:id/kill") { httpReq: KillRunHttpRequest =>
    val req = KillRunRequest(RunId(httpReq.id))
    client.killRun(req).map(_ => response.ok)
  }

  delete("/api/v1/run/:id") { httpReq: DeleteRunHttpRequest =>
    val req = DeleteRunRequest(RunId(httpReq.id))
    client.deleteRun(req).map(_ => response.ok)
  }

  get("/api/v1/run/:id/metrics/:node") { httpReq: ListMetricsHttpRequest =>
    val req = GetRunRequest(RunId(httpReq.id))
    client.getRun(req).map(_.result).map {
      case Some(run) => run.state.nodes.find(_.name == httpReq.node).flatMap(_.result).map(_.metrics)
      case None => response.notFound
    }
  }

  get("/api/v1/run/:id/logs/:node/:classifier") { httpReq: ListLogsHttpRequest =>
    val req = ListLogsRequest(
      runId = RunId(httpReq.id),
      nodeName = httpReq.node,
      classifier = Some(httpReq.classifier),
      limit = httpReq.limit,
      since = httpReq.since.map(_.getMillis))
    client.listLogs(req).map(_.results).map { logs =>
      if (httpReq.download) {
        response
          .ok(logs.map(_.message).mkString("\n"))
          .header("Content-Disposition", s"attachment; filename=${req.runId.value}-${req.nodeName}-${httpReq.classifier}.txt")
      } else {
        logs
      }
    }
  }
}

case class ListRunsHttpRequest(
  @QueryParam owner: Option[String],
  @QueryParam name: Option[String],
  @QueryParam workflow: Option[String],
  @QueryParam status: Option[String],
  @QueryParam parent: Option[String],
  @QueryParam clonedFrom: Option[String],
  @QueryParam tags: Option[String],
  @QueryParam @Min(1) page: Int = 1,
  @QueryParam @Min(0) @Max(50) perPage: Int = 15)

case class GetRunHttpRequest(@RouteParam id: String, @QueryParam download: Boolean = false)

case class ListArtifactsHttpRequest(@RouteParam id: String, @RouteParam node: String)

case class ListMetricsHttpRequest(@RouteParam id: String, @RouteParam node: String)

case class DeleteRunHttpRequest(@RouteParam id: String)

case class KillRunHttpRequest(@RouteParam id: String)

case class ListWorkflowsHttpRequest(
  @QueryParam owner: Option[String],
  @QueryParam name: Option[String],
  @QueryParam @Min(1) page: Int = 1,
  @QueryParam @Min(0) @Max(50) perPage: Int = 15)

case class GetWorkflowHttpRequest(
  @RouteParam id: String,
  @RouteParam version: Option[String],
  @QueryParam download: Boolean = false)

case class DeleteWorkflowHttpRequest(@RouteParam id: String)

case class ListLogsHttpRequest(
  @RouteParam id: String,
  @RouteParam node: String,
  @RouteParam classifier: String,
  @QueryParam @Min(1) limit: Option[Int],
  @QueryParam @Min(0) since: Option[DateTime],
  @QueryParam download: Boolean = false)

case class ResultListResponse[T](results: Seq[T], totalCount: Int)