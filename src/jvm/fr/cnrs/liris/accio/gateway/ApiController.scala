/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.gateway

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.{JsonIgnoreBody, QueryParam, RouteParam}
import com.twitter.finatra.validation.{Max, Min}
import com.twitter.io.Reader
import com.twitter.util.{Future, Return, Throw}
import fr.cnrs.liris.accio.agent.{ListOperatorsRequest, _}
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.common.util.StringUtils.explode
import org.joda.time.DateTime

@Singleton
class ApiController @Inject()(client: AgentService$FinagleClient) extends Controller {
  private[this] val user = User("vprimault")

  get("/api/v1") { _: Request =>
    client.getCluster(GetClusterRequest()).map { resp =>
      IndexResponse(resp.clusterName, resp.version)
    }
  }

  get("/api/v1/operator") { httpReq: ListOperatorsHttpRequest =>
    val req = ListOperatorsRequest(httpReq.includeDeprecated)
    client.listOperators(req).map { resp =>
      ResultListResponse(resp.results, resp.results.size)
    }
  }

  get("/api/v1/operator/:name") { httpReq: GetOperatorHttpRequest =>
    val req = GetOperatorRequest(httpReq.name)
    client.getOperator(req).map(_.result)
  }

  get("/api/v1/workflow") { httpReq: ListWorkflowsHttpRequest =>
    val offset = (httpReq.page - 1) * httpReq.perPage
    val req = ListWorkflowsRequest(
      name = httpReq.name,
      owner = httpReq.owner,
      q = httpReq.q,
      limit = Some(httpReq.perPage),
      offset = Some(offset))
    client.listWorkflows(req).map { resp =>
      ResultListResponse(resp.results, resp.totalCount)
    }
  }

  post("/api/v1/workflow/:id") { httpReq: UpdateWorkflowHttpRequest =>
    readBody(httpReq.request).flatMap { content =>
      val req = ParseWorkflowRequest(new String(content), None)
      client.parseWorkflow(req)
    }.flatMap { resp =>
      resp.workflow match {
        case None =>
          val res = ParseErrorResponse(resp.warnings.map(_.message), resp.errors.map(_.message))
          Future.value(response.badRequest(res))
        case Some(workflow) =>
          val req = PushWorkflowRequest(workflow, user)
          client.pushWorkflow(req).map { resp =>
            UpdateWorkflowResponse(resp.workflow.version.get)
          }
      }
    }
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
      status = httpReq.status.flatMap(v => TaskState.valueOf(v)).toSet,
      tags = explode(httpReq.tags, ","),
      q = httpReq.q,
      limit = Some(httpReq.perPage),
      offset = Some(offset))
    client.listRuns(req).map { resp =>
      ResultListResponse(resp.results, resp.totalCount)
    }
  }

  post("/api/v1/run") { httpReq: CreateRunHttpRequest =>
    readBody(httpReq.request).flatMap { content =>
      val req = ParseRunRequest(content, httpReq.request.params, None)
      client.parseRun(req)
    }.flatMap { resp =>
      resp.run match {
        case None =>
          val res = ParseErrorResponse(resp.warnings.map(_.message), resp.errors.map(_.message))
          Future.value(response.badRequest(res))
        case Some(run) =>
          val req = CreateRunRequest(run, user)
          client.createRun(req).map(_.ids)
      }
    }
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
    client.killRun(req).liftToTry.map {
      case Return(_) => response.ok
      case Throw(e: UnknownRunException) => response.notFound
    }
  }

  post("/api/v1/run/:id") { httpReq: UpdateRunHttpRequest =>
    val req = UpdateRunRequest(RunId(httpReq.id), httpReq.name, httpReq.notes, httpReq.tags)
    client.updateRun(req).liftToTry.map {
      case Return(_) => response.ok
      case Throw(e: UnknownRunException) => response.notFound
    }
  }

  delete("/api/v1/run/:id") { httpReq: DeleteRunHttpRequest =>
    val req = DeleteRunRequest(RunId(httpReq.id))
    client.deleteRun(req).liftToTry.map {
      case Return(_) => response.ok
      case Throw(e: UnknownRunException) => response.notFound
    }
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

  private def readBody(httpReq: Request): Future[String] = {
    Reader.readAll(httpReq.reader).map { buf =>
      val bytes = Array.ofDim[Byte](buf.length)
      buf.write(bytes, 0)
      new String(bytes)
    }
  }
}

case class IndexResponse(clusterName: String, version: String)

case class ListOperatorsHttpRequest(@QueryParam includeDeprecated: Boolean = false)

case class GetOperatorHttpRequest(@RouteParam name: String)

@JsonIgnoreBody
case class CreateRunHttpRequest(request: Request)

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

@JsonIgnoreBody
case class UpdateWorkflowHttpRequest(@RouteParam id: String, request: Request)

case class UpdateWorkflowResponse(version: String)

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

case class DeleteWorkflowHttpRequest(@RouteParam id: String)

case class ListLogsHttpRequest(
  @RouteParam id: String,
  @RouteParam node: String,
  @RouteParam classifier: String,
  @QueryParam @Min(0) limit: Option[Int],
  @QueryParam since: Option[DateTime],
  @QueryParam download: Boolean = false)

case class ResultListResponse[T](results: Seq[T], totalCount: Int)

case class ParseErrorResponse(warnings: Seq[String], errors: Seq[String])