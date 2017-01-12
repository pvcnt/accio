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

package fr.cnrs.liris.accio.agent

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.Service
import com.twitter.finatra.thrift.Controller
import com.twitter.inject.Injector
import fr.cnrs.liris.accio.core.application.handler._
import fr.cnrs.liris.accio.thrift.agent.MasterService._
import fr.cnrs.liris.accio.thrift.agent._

@Singleton
class MasterThriftController @Inject()(injector: Injector)
  extends Controller with MasterService.BaseServiceIface {

  override val getOperator: Service[GetOperator.Args, GetOperator.Result] =
    handle(GetOperator) { args: GetOperator.Args =>
      injector.instance[GetOperatorHandler].handle(args.req)
    }

  override val listOperators: Service[ListOperators.Args, ListOperators.Result] =
    handle(ListOperators) { args: ListOperators.Args =>
      injector.instance[ListOperatorsHandler].handle(args.req)
    }

  override val pushWorkflow = handle(PushWorkflow) { args: PushWorkflow.Args =>
    injector.instance[PushWorkflowHandler].handle(args.req)
  }

  override val getWorkflow = handle(GetWorkflow) { args: GetWorkflow.Args =>
    injector.instance[GetWorkflowHandler].handle(args.req)
  }

  override val listWorkflows = handle(ListWorkflows) { args: ListWorkflows.Args =>
    injector.instance[ListWorkflowsHandler].handle(args.req)
  }

  override val deleteWorkflow = handle(DeleteWorkflow) { args: DeleteWorkflow.Args =>
    injector.instance[DeleteWorkflowHandler].handle(args.req)
  }

  override val createRun = handle(CreateRun) { args: CreateRun.Args =>
    injector.instance[CreateRunHandler].handle(args.req)
  }

  override val getRun = handle(GetRun) { args: GetRun.Args =>
    injector.instance[GetRunHandler].handle(args.req)
  }

  override val listRuns = handle(ListRuns) { args: ListRuns.Args =>
    injector.instance[ListRunsHandler].handle(args.req)
  }

  override val deleteRun = handle(DeleteRun) { args: DeleteRun.Args =>
    injector.instance[DeleteRunHandler].handle(args.req)
  }

  override val killRun = handle(KillRun) { args: KillRun.Args =>
    injector.instance[KillRunHandler].handle(args.req)
  }
}