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
import fr.cnrs.liris.common.util.StringUtils.{explode, maybe}

@Singleton
final class ApiController @Inject()(client: AgentService.MethodPerEndpoint) extends Controller {
  get("/api/v1") { _: Request =>
    client.getCluster(GetClusterRequest()).map { resp =>
      IndexResponse(resp.clusterName, resp.version)
    }
  }

  get("/api/v1/operator") { httpReq: ListOperatorsHttpRequest =>
    val req = ListOperatorsRequest(httpReq.includeDeprecated)
    client.listOperators(req).map { resp =>
      ResultListResponse(resp.operators, resp.operators.size)
    }
  }

  get("/api/v1/operator/:name") { httpReq: GetOperatorHttpRequest =>
    val req = GetOperatorRequest(httpReq.name)
    client.getOperator(req).map(_.operator)
  }

  get("/api/v1/workflow") { httpReq: ListWorkflowsHttpRequest =>
    val offset = (httpReq.page - 1) * httpReq.perPage
    val req = ListWorkflowsRequest(
      name = httpReq.name,
      owner = httpReq.owner,
      q = httpReq.q,
      limit = Some(httpReq.perPage),
      offset = Some(offset))
    client
      .listWorkflows(req)
      .map(resp => ResultListResponse(resp.workflows, resp.totalCount))
  }

  get("/api/v1/workflow/:id") { httpReq: GetWorkflowHttpRequest =>
    val req = GetWorkflowRequest(httpReq.id, httpReq.version.flatMap(maybe))
    client
      .getWorkflow(req)
      .map { resp =>
        if (httpReq.download) {
          response
            .ok(resp.workflow)
            .header("Content-Disposition", s"attachment; filename=${resp.workflow.id}.json")
        } else {
          response.ok(resp.workflow)
        }
      }
  }

  get("/api/v1/run") { httpReq: ListRunsHttpRequest =>
    val offset = (httpReq.page - 1) * httpReq.perPage
    val req = ListRunsRequest(
      name = httpReq.name,
      owner = httpReq.owner,
      workflowId = httpReq.workflow,
      parent = httpReq.parent,
      clonedFrom = httpReq.clonedFrom,
      status = httpReq.status.flatMap(v => TaskState.valueOf(v)).toSet,
      tags = explode(httpReq.tags, ","),
      q = httpReq.q,
      limit = Some(httpReq.perPage),
      offset = Some(offset))
    client
      .listRuns(req)
      .map(resp => ResultListResponse(resp.runs, resp.totalCount))
  }

  get("/api/v1/run/:id") { httpReq: GetRunHttpRequest =>
    val req = GetRunRequest(httpReq.id)
    client.getRun(req).map { resp =>
      if (httpReq.download) {
        // We download the entirety of the run, including artifacts and metrics.
        response
          .ok(resp.run)
          .header("Content-Disposition", s"attachment; filename=run-${resp.run.id}.json")
      } else {
        // In other cases, we do not transmit node result.
        resp.run.copy(state = resp.run.state.copy(nodes = resp.run.state.nodes.map(_.unsetResult)))
      }
    }
  }

  get("/api/v1/run/:id/artifacts/:node") { httpReq: ListArtifactsHttpRequest =>
    client
      .getRun(GetRunRequest(httpReq.id))
      .map { resp =>
        resp.run.state.nodes.find(_.name == httpReq.node).flatMap(_.result).map(_.artifacts)
      }
  }

  post("/api/v1/run/:id/kill") { httpReq: KillRunHttpRequest =>
    client
      .killRun(KillRunRequest(httpReq.id))
      .map(_ => response.ok)
  }

  post("/api/v1/run/:id") { httpReq: UpdateRunHttpRequest =>
    client
      .updateRun(UpdateRunRequest(httpReq.id, httpReq.name, httpReq.notes, httpReq.tags))
      .map(resp => response.ok(resp.run))
  }

  delete("/api/v1/run/:id") { httpReq: DeleteRunHttpRequest =>
    client
      .deleteRun(DeleteRunRequest(httpReq.id))
      .map(_ => response.ok)
  }

  get("/api/v1/run/:id/metrics/:node") { httpReq: ListMetricsHttpRequest =>
    client
      .getRun(GetRunRequest(httpReq.id))
      .map { resp =>
        resp.run.state.nodes.find(_.name == httpReq.node).flatMap(_.result).map(_.metrics)
      }
  }

  get("/api/v1/run/:id/logs/:node/:kind") { httpReq: ListLogsHttpRequest =>
    val req = ListLogsRequest(
      runId = httpReq.id,
      nodeName = httpReq.node,
      kind = httpReq.kind,
      skip = httpReq.skip)
    client
      .listLogs(req)
      .map(_.results)
      .map { logs =>
        if (httpReq.download) {
          response
            .ok(logs.mkString("\n"))
            .header("Content-Disposition", s"attachment; filename=logs-${req.runId}-${req.nodeName}-${httpReq.kind}.txt")
        } else {
          logs
        }
      }
  }
}

case class IndexResponse(clusterName: String, version: String)

case class ListOperatorsHttpRequest(@QueryParam includeDeprecated: Boolean = false)

case class GetOperatorHttpRequest(@RouteParam name: String)

case class ListRunsHttpRequest(
  @QueryParam owner: Option[String],
  @QueryParam name: Option[String],
  @QueryParam workflow: Option[String],
  @QueryParam status: Option[String],
  @QueryParam parent: Option[String],
  @QueryParam clonedFrom: Option[String],
  @QueryParam tags: Option[String],
  @QueryParam q: Option[String],
  @QueryParam @Min(1) page: Int = 1,
  @QueryParam @Min(0) @Max(50) perPage: Int = 15)

case class GetRunHttpRequest(@RouteParam id: String, @QueryParam download: Boolean = false)

case class UpdateRunHttpRequest(
  @RouteParam id: String,
  name: Option[String],
  notes: Option[String],
  tags: Set[String] = Set.empty)

case class ListArtifactsHttpRequest(@RouteParam id: String, @RouteParam node: String)

case class ListMetricsHttpRequest(@RouteParam id: String, @RouteParam node: String)

case class DeleteRunHttpRequest(@RouteParam id: String)

case class KillRunHttpRequest(@RouteParam id: String)

case class ListWorkflowsHttpRequest(
  @QueryParam owner: Option[String],
  @QueryParam name: Option[String],
  @QueryParam q: Option[String],
  @QueryParam @Min(1) page: Int = 1,
  @QueryParam @Min(0) @Max(50) perPage: Int = 15)

case class GetWorkflowHttpRequest(
  @RouteParam id: String,
  @RouteParam version: Option[String],
  @QueryParam download: Boolean = false)

case class ListLogsHttpRequest(
  @RouteParam id: String,
  @RouteParam node: String,
  @RouteParam kind: String,
  @QueryParam @Min(0) skip: Option[Int],
  @QueryParam download: Boolean = false)

case class ResultListResponse[T](results: Seq[T], totalCount: Int)

case class ParseErrorResponse(warnings: Seq[String], errors: Seq[String])