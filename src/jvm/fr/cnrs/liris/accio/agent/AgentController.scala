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

package fr.cnrs.liris.accio.agent

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.Service
import com.twitter.finatra.thrift.Controller
import com.twitter.inject.Injector
import com.twitter.scrooge.ThriftException
import fr.cnrs.liris.accio.agent.AgentService.{CompleteTask, RegisterWorker, _}
import fr.cnrs.liris.accio.agent.api._
import fr.cnrs.liris.accio.agent.master._
import fr.cnrs.liris.accio.agent.worker._

import scala.util.control.NonFatal

@Singleton
class AgentController @Inject()(injector: Injector) extends Controller with AgentService.BaseServiceIface {
  override val getOperator: Service[GetOperator.Args, GetOperator.Result] =
    handle(GetOperator) { args: GetOperator.Args =>
      injector.instance[GetOperatorHandler].handle(args.req)
    }

  override val listOperators = handle(ListOperators) { args: ListOperators.Args =>
    reportException(injector.instance[ListOperatorsHandler].handle(args.req))
  }

  override val pushWorkflow = handle(PushWorkflow) { args: PushWorkflow.Args =>
    reportException(injector.instance[PushWorkflowHandler].handle(args.req))
  }

  override val getWorkflow = handle(GetWorkflow) { args: GetWorkflow.Args =>
    reportException(injector.instance[GetWorkflowHandler].handle(args.req))
  }

  override val listWorkflows = handle(ListWorkflows) { args: ListWorkflows.Args =>
    reportException(injector.instance[ListWorkflowsHandler].handle(args.req))
  }

  override val parseWorkflow = handle(ParseWorkflow) { args: ParseWorkflow.Args =>
    reportException(injector.instance[ParseWorkflowHandler].handle(args.req))
  }

  override val createRun = handle(CreateRun) { args: CreateRun.Args =>
    reportException(injector.instance[CreateRunHandler].handle(args.req))
  }

  override val getRun = handle(GetRun) { args: GetRun.Args =>
    reportException(injector.instance[GetRunHandler].handle(args.req))
  }

  override val listRuns = handle(ListRuns) { args: ListRuns.Args =>
    reportException(injector.instance[ListRunsHandler].handle(args.req))
  }

  override val deleteRun = handle(DeleteRun) { args: DeleteRun.Args =>
    reportException(injector.instance[DeleteRunHandler].handle(args.req))
  }

  override val killRun = handle(KillRun) { args: KillRun.Args =>
    reportException(injector.instance[KillRunHandler].handle(args.req))
  }

  override val updateRun = handle(UpdateRun) { args: UpdateRun.Args =>
    reportException(injector.instance[UpdateRunHandler].handle(args.req))
  }

  override val parseRun = handle(ParseRun) { args: ParseRun.Args =>
    reportException(injector.instance[ParseRunHandler].handle(args.req))
  }

  override val listLogs = handle(ListLogs) { args: ListLogs.Args =>
    reportException(injector.instance[ListLogsHandler].handle(args.req))
  }

  override val getCluster = handle(GetCluster) { args: GetCluster.Args =>
    injector.instance[GetClusterHandler].handle(args.req)
  }

  override val heartbeatWorker = handle(HeartbeatWorker) { args: HeartbeatWorker.Args =>
    reportException(injector.instance[HeartbeatWorkerHandler].handle(args.req))
  }

  override val startTask = handle(StartTask) { args: StartTask.Args =>
    reportException(injector.instance[StartTaskHandler].handle(args.req))
  }

  override val streamTaskLogs = handle(StreamTaskLogs) { args: StreamTaskLogs.Args =>
    reportException(injector.instance[StreamTaskLogsHandler].handle(args.req))
  }

  override val completeTask = handle(CompleteTask) { args: CompleteTask.Args =>
    reportException(injector.instance[CompleteTaskHandler].handle(args.req))
  }

  override val registerWorker =handle(RegisterWorker) { args: RegisterWorker.Args =>
    reportException(injector.instance[RegisterWorkerHandler].handle(args.req))
  }

  override val unregisterWorker = handle(UnregisterWorker) { args: UnregisterWorker.Args =>
    reportException(injector.instance[UnregisterWorkerHandler].handle(args.req))
  }

  override val lostTask = handle(LostTask) { args: LostTask.Args =>
    reportException(injector.instance[LostTaskHandler].handle(args.req))
  }

  override val assignTask = handle(AssignTask) { args: AssignTask.Args =>
    reportException(injector.instance[AssignTaskHandler].handle(args.req))
  }

  override val killTask = handle(KillTask) { args: KillTask.Args =>
    reportException(injector.instance[KillTaskHandler].handle(args.req))
  }

  override val heartbeatExecutor = handle(HeartbeatExecutor) { args: HeartbeatExecutor.Args =>
    reportException(injector.instance[HeartbeatExecutorHandler].handle(args.req))
  }

  override val startExecutor = handle(StartExecutor) { args: StartExecutor.Args =>
    reportException(injector.instance[StartExecutorHandler].handle(args.req))
  }

  override val streamExecutorLogs = handle(StreamExecutorLogs) { args: StreamExecutorLogs.Args =>
    reportException(injector.instance[StreamExecutorLogsHandler].handle(args.req))
  }

  override val stopExecutor = handle(StopExecutor) { args: StopExecutor.Args =>
    reportException(injector.instance[StopExecutorHandler].handle(args.req))
  }

  private def reportException[T](f: => T): T = try {
    f
  } catch {
    case e: ThriftException => throw e // Rethrow an exception handled by Thrift.
    case NonFatal(e) =>
      logger.error("Uncaught exception", e)
      throw e
  }
}