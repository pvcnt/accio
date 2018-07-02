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

package fr.cnrs.liris.accio.server

import com.google.inject.{Inject, Singleton}
import com.twitter.finatra.thrift.Controller
import com.twitter.util.Future
import fr.cnrs.liris.accio.discovery.OpRegistry
import fr.cnrs.liris.accio.domain.thrift.ThriftAdapter
import fr.cnrs.liris.accio.scheduler.Scheduler
import fr.cnrs.liris.accio.server.AccioService._
import fr.cnrs.liris.accio.validation.{ValidationResult, WorkflowValidator}
import fr.cnrs.liris.accio.version.Version
import fr.cnrs.liris.infra.thriftserver.{FieldViolation, ServerException, UserInfo}
import fr.cnrs.liris.lumos.transport.EventTransport

@Singleton
final class AccioServiceController @Inject()(
  validator: WorkflowValidator,
  submitService: SubmitWorkflowService,
  scheduler: Scheduler,
  transport: EventTransport,
  registry: OpRegistry)
  extends Controller with AccioService.ServicePerEndpoint {

  override val getInfo = handle(GetInfo) { args: GetInfo.Args =>
    Future {
      GetInfoResponse(Version.Current.toString)
    }
  }

  override val getOperator = handle(GetOperator) { args: GetOperator.Args =>
    registry.get(args.req.name) match {
      case None => Future.exception(ServerException.NotFound("operators", args.req.name))
      case Some(opDef) => Future.value(GetOperatorResponse(ThriftAdapter.toThrift(opDef)))
    }
  }

  override val listOperators = handle(ListOperators) { args: ListOperators.Args =>
    Future {
      var results = registry.ops.toSeq.sortBy(_.name)
      if (!args.req.includeDeprecated) {
        results = results.filter(_.deprecation.isEmpty)
      }
      ListOperatorsResponse(results.map(ThriftAdapter.toThrift))
    }
  }

  override val validateWorkflow = handle(ValidateWorkflow) { args: ValidateWorkflow.Args =>
    Future {
      val workflow = validator.prepare(ThriftAdapter.toDomain(args.req.workflow), UserInfo.current.map(_.name))
      val result = validator.validate(workflow)
      ValidateWorkflowResponse(result.errors.map(toThrift), result.warnings.map(toThrift))
    }
  }

  override val submitWorkflow = handle(SubmitWorkflow) { args: SubmitWorkflow.Args =>
    val workflow = validator.prepare(ThriftAdapter.toDomain(args.req.workflow), UserInfo.current.map(_.name))
    val result = validator.validate(workflow)
    if (result.isInvalid) {
      Future.exception(ServerException.InvalidArgument(result.errors.map(v => ServerException.FieldViolation(v.message, v.field))))
    } else {
      submitService
        .apply(workflow)
        .map(workflows => SubmitWorkflowResponse(workflow.name, workflows.map(_.name)))
    }
  }

  override val killWorkflow = handle(KillWorkflow) { args: KillWorkflow.Args =>
    scheduler.kill(args.req.name).map(_ => KillWorkflowResponse())
  }

  private def toThrift(obj: ValidationResult.FieldViolation) = FieldViolation(obj.message, obj.field)

  /*override val listLogs = handle(ListLogs) { args: ListLogs.Args =>
    storage.jobs
      .get(args.req.job)
      .flatMap {
        case None => Future.exception(Errors.notFound("job", args.req.job))
        case Some(job) =>
          job.status.tasks.toSeq.flatten.find(_.name == args.req.step) match {
            case None => Future.exception(Errors.notFound("task", s"${args.req.job}/${args.req.step}"))
            case Some(task) =>
              val processName = Process.name(args.req.job, task.name)
              scheduler.getLogs(processName, args.req.kind, skip = args.req.skip, tail = args.req.tail)
          }
      }
      .map(lines => ListLogsResponse(lines))
  }*/
}