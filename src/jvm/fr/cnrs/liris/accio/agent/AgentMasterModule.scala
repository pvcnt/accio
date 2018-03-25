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

package fr.cnrs.liris.accio.agent

import com.google.inject.TypeLiteral
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.FuturePool
import fr.cnrs.liris.accio.agent.handler._
import fr.cnrs.liris.accio.agent.handler.api._
import fr.cnrs.liris.accio.framework.dsl.inject.DslModule
import fr.cnrs.liris.accio.framework.scheduler.inject.SchedulerModule
import fr.cnrs.liris.accio.framework.storage.inject.StorageModule
import fr.cnrs.liris.accio.runtime.commandbus.Handler
import net.codingwell.scalaguice.ScalaMultibinder

/**
 * Guice module provisioning services for the Accio agent.
 */
object AgentMasterModule extends TwitterModule {
  override def modules = Seq(SchedulerModule, StorageModule, DslModule)

  override def configure(): Unit = {
    // Bind command handlers.
    val handlers = ScalaMultibinder.newSetBinder(binder, new TypeLiteral[Handler[_, _]] {})
    handlers.addBinding.to[CreateRunHandler]
    handlers.addBinding.to[DeleteRunHandler]
    handlers.addBinding.to[GetClusterHandler]
    handlers.addBinding.to[ListAgentsHandler]
    handlers.addBinding.to[GetOperatorHandler]
    handlers.addBinding.to[GetRunHandler]
    handlers.addBinding.to[GetWorkflowHandler]
    handlers.addBinding.to[KillRunHandler]
    handlers.addBinding.to[ListLogsHandler]
    handlers.addBinding.to[ListOperatorsHandler]
    handlers.addBinding.to[ListRunsHandler]
    handlers.addBinding.to[ListWorkflowsHandler]
    handlers.addBinding.to[ParseRunHandler]
    handlers.addBinding.to[ParseWorkflowHandler]
    handlers.addBinding.to[PushWorkflowHandler]
    handlers.addBinding.to[UpdateRunHandler]

    handlers.addBinding.to[CompleteTaskHandler]
    handlers.addBinding.to[HeartbeatWorkerHandler]
    handlers.addBinding.to[LostTaskHandler]
    handlers.addBinding.to[RegisterWorkerHandler]
    handlers.addBinding.to[StartTaskHandler]
    handlers.addBinding.to[StreamTaskLogsHandler]
    handlers.addBinding.to[UnregisterWorkerHandler]
  }

  override def singletonStartup(injector: Injector): Unit = {
    val observer = injector.instance[LostWorkerObserver]
    FuturePool.unboundedPool(observer.run())
  }

  override def singletonShutdown(injector: Injector): Unit = {
    injector.instance[LostWorkerObserver].kill()
  }
}