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

import com.google.inject.Module
import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.filters._
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.util.FuturePool
import fr.cnrs.liris.accio.agent.handler.{LostExecutorsObserver, LostWorkerObserver, WorkerLifecycle}
import fr.cnrs.liris.accio.core.dsl.inject.DslModule
import fr.cnrs.liris.accio.core.filesystem.inject.FileSystemModule
import fr.cnrs.liris.accio.core.scheduler.inject.SchedulerModule
import fr.cnrs.liris.accio.core.storage.inject.StorageModule
import fr.cnrs.liris.accio.runtime.logging.LogbackConfigurator
import fr.cnrs.liris.privamov.ops.OpsModule

object AgentServerMain extends AgentServer

class AgentServer extends ThriftServer with LogbackConfigurator {
  override protected def modules: Seq[Module] = Seq(
    FileSystemModule,
    SchedulerModule,
    StorageModule,
    DslModule,
    AgentModule,
    OpsModule)

  override protected def configureThrift(router: ThriftRouter): Unit = {
    router
      .filter[LoggingMDCFilter]
      .filter[TraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter[AccessLoggingFilter]
      .filter[StatsFilter]
      .add[AgentController]
  }

  override protected def start(): Unit = {
    if (AgentModule.masterFlag()) {
      val observer = injector.instance[LostWorkerObserver]
      FuturePool.unboundedPool(observer.run())
      onExit {
        observer.kill()
      }
    }
    if (AgentModule.workerFlag()) {
      val lifecycle = injector.instance[WorkerLifecycle]
      lifecycle.register()
      val observer = injector.instance[LostExecutorsObserver]
      FuturePool.unboundedPool(observer.run())
      onExit {
        lifecycle.unregister()
        observer.kill()
      }
    }
  }
}