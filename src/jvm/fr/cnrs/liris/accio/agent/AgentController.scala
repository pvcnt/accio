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
import com.twitter.finatra.thrift.Controller
import com.twitter.scrooge.ThriftException
import com.twitter.util.Future
import fr.cnrs.liris.accio.agent.AgentService.{CompleteTask, RegisterWorker, _}
import fr.cnrs.liris.accio.runtime.commandbus.CommandBus

import scala.util.control.NonFatal

@Singleton
class AgentController @Inject()(commandBus: CommandBus) extends Controller with AgentService.BaseServiceIface {
  /**
   * Public RPC endpoints used by clients to communicate with masters.
   */
  override val getOperator = handle(GetOperator) { args: GetOperator.Args => handleRequest(args.req) }
  override val listOperators = handle(ListOperators) { args: ListOperators.Args => handleRequest(args.req) }
  override val pushWorkflow = handle(PushWorkflow) { args: PushWorkflow.Args => handleRequest(args.req) }
  override val getWorkflow = handle(GetWorkflow) { args: GetWorkflow.Args => handleRequest(args.req) }
  override val listWorkflows = handle(ListWorkflows) { args: ListWorkflows.Args => handleRequest(args.req) }
  override val parseWorkflow = handle(ParseWorkflow) { args: ParseWorkflow.Args => handleRequest(args.req) }
  override val createRun = handle(CreateRun) { args: CreateRun.Args => handleRequest(args.req) }
  override val getRun = handle(GetRun) { args: GetRun.Args => handleRequest(args.req) }
  override val listRuns = handle(ListRuns) { args: ListRuns.Args => handleRequest(args.req) }
  override val deleteRun = handle(DeleteRun) { args: DeleteRun.Args => handleRequest(args.req) }
  override val killRun = handle(KillRun) { args: KillRun.Args => handleRequest(args.req) }
  override val updateRun = handle(UpdateRun) { args: UpdateRun.Args => handleRequest(args.req) }
  override val parseRun = handle(ParseRun) { args: ParseRun.Args => handleRequest(args.req) }
  override val listLogs = handle(ListLogs) { args: ListLogs.Args => handleRequest(args.req) }
  override val getCluster = handle(GetCluster) { args: GetCluster.Args => handleRequest(args.req) }
  override val listAgents = handle(ListAgents) { args: ListAgents.Args => handleRequest(args.req) }
  override val getDataset = handle(GetDataset) { args: GetDataset.Args => handleRequest(args.req) }

  /**
   * RPC endpoints used by workers to communicate with their master.
   */
  override val heartbeatWorker = handle(HeartbeatWorker) { args: HeartbeatWorker.Args => handleRequest(args.req) }
  override val startTask = handle(StartTask) { args: StartTask.Args => handleRequest(args.req) }
  override val streamTaskLogs = handle(StreamTaskLogs) { args: StreamTaskLogs.Args => handleRequest(args.req) }
  override val completeTask = handle(CompleteTask) { args: CompleteTask.Args => handleRequest(args.req) }
  override val registerWorker = handle(RegisterWorker) { args: RegisterWorker.Args => handleRequest(args.req) }
  override val unregisterWorker = handle(UnregisterWorker) { args: UnregisterWorker.Args => handleRequest(args.req) }
  override val lostTask = handle(LostTask) { args: LostTask.Args => handleRequest(args.req) }

  /**
   * RPC endpoints used by master to communicate with their workers, and executors to communicate with their workers.
   */
  override val assignTask = handle(AssignTask) { args: AssignTask.Args => handleRequest(args.req) }
  override val killTask = handle(KillTask) { args: KillTask.Args => handleRequest(args.req) }

  /**
   * RPC endpoints used by executors to communicate with their worker.
   */
  override val heartbeatExecutor = handle(HeartbeatExecutor) { args: HeartbeatExecutor.Args => handleRequest(args.req) }
  override val startExecutor = handle(StartExecutor) { args: StartExecutor.Args => handleRequest(args.req) }
  override val streamExecutorLogs = handle(StreamExecutorLogs) { args: StreamExecutorLogs.Args => handleRequest(args.req) }
  override val stopExecutor = handle(StopExecutor) { args: StopExecutor.Args => handleRequest(args.req) }

  /**
   * Handler a request using a command bus and return a future for the response.
   *
   * @param req Request to handle.
   * @tparam T Result type.
   */
  private def handleRequest[T](req: Any): Future[T] =
    try {
      commandBus.handle(req).asInstanceOf[Future[T]]
    } catch {
      case e: ThriftException => throw e // Rethrow an exception handled by Thrift.
      case NonFatal(e) =>
        logger.error("Uncaught exception", e)
        throw e
    }
}