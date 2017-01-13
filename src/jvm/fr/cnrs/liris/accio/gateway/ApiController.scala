/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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
import fr.cnrs.liris.accio.agent.AgentService
import fr.cnrs.liris.accio.core.application.handler._
import fr.cnrs.liris.accio.core.domain._

@Singleton
class ApiController @Inject()(client: AgentService.FinagledClient) extends Controller {
  get("/health") { req: Request =>
    response.ok("OK")
  }

  get("/v1/workflow") { httpReq: ListWorkflowsHttpRequest =>
    val offset = (httpReq.page - 1) * httpReq.perPage
    val req = ListWorkflowsRequest(limit = Some(httpReq.perPage), offset = Some(offset))
    client.listWorkflows(req)
  }

  post("/v1/workflow") { req: Request =>
    response.badRequest
  }

  get("/v1/workflow/:id") { httpReq: GetWorkflowHttpRequest =>
    val req = GetWorkflowRequest(WorkflowId(httpReq.id), httpReq.version)
    client.getWorkflow(req).map(_.result)
  }

  get("/v1/run") { httpReq: ListRunsHttpRequest =>
    val offset = (httpReq.page - 1) * httpReq.perPage
    val req = ListRunsRequest(limit = Some(httpReq.perPage), offset = Some(offset))
    client.listRuns(req)
  }

  post("/v1/run") { req: Request =>
    response.badRequest
  }

  get("/v1/run/:id") { httpReq: GetRunHttpRequest =>
    val req = GetRunRequest(RunId(httpReq.id))
    client.getRun(req).map(_.result)
  }

  delete("/v1/run/:id") { httpReq: DeleteRunHttpRequest =>
    val req = DeleteRunRequest(RunId(httpReq.id))
    client.deleteRun(req).map(_ => response.ok)
  }

  post("/v1/run/:id/kill") { httpReq: KillRunHttpRequest =>
    val req = KillRunRequest(RunId(httpReq.id))
    client.killRun(req).map(_ => response.ok)
  }
}

case class ListRunsHttpRequest(
  @QueryParam @Min(1) page: Int = 1,
  @QueryParam @Min(0) @Max(50) perPage: Int = 15)

case class GetRunHttpRequest(@RouteParam id: String)

case class DeleteRunHttpRequest(@RouteParam id: String)

case class KillRunHttpRequest(@RouteParam id: String)

case class ListWorkflowsHttpRequest(
  @QueryParam @Min(1) page: Int = 1,
  @QueryParam @Min(0) @Max(50) perPage: Int = 15)

case class GetWorkflowHttpRequest(@RouteParam id: String, @RouteParam version: Option[String])

case class DeleteWorkflowHttpRequest(@RouteParam id: String)
