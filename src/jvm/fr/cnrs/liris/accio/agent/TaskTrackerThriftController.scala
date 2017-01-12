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
import fr.cnrs.liris.accio.core.application.handler.{CompletedTaskHandler, HeartbeatTaskHandler, RegisterExecutorHandler, StreamLogsHandler}
import fr.cnrs.liris.accio.thrift.agent.TaskTrackerService._
import fr.cnrs.liris.accio.thrift.agent._

@Singleton
class TaskTrackerThriftController @Inject()(injector: Injector)
  extends Controller with TaskTrackerService.BaseServiceIface {

  override val heartbeat: Service[Heartbeat.Args, Heartbeat.Result] =
    handle(Heartbeat) { args: Heartbeat.Args =>
      injector.instance[HeartbeatTaskHandler].handle(args.req)
    }

  override val register: Service[Register.Args, Register.Result] = {
    handle(Register) { args: Register.Args =>
      injector.instance[RegisterExecutorHandler].handle(args.req)
    }
  }

  override val stream: Service[Stream.Args, Stream.Result] = {
    handle(Stream) { args: Stream.Args =>
      injector.instance[StreamLogsHandler].handle(args.req)
    }
  }

  override val completed: Service[Completed.Args, Completed.Result] = {
    handle(Completed) { args: Completed.Args =>
      injector.instance[CompletedTaskHandler].handle(args.req)
    }
  }
}