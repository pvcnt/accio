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

import com.google.inject.{Module, Provides, Singleton, TypeLiteral}
import com.twitter.finagle.Thrift
import com.twitter.finagle.service._
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.{Duration, FuturePool}
import fr.cnrs.liris.accio.runtime.commandbus.Handler
import fr.cnrs.liris.accio.agent.config._
import fr.cnrs.liris.accio.agent.handler._
import fr.cnrs.liris.accio.core.api.thrift.Resource
import fr.cnrs.liris.accio.core.filesystem.inject.FileSystemModule
import fr.cnrs.liris.privamov.ops.OpsModule
import net.codingwell.scalaguice.ScalaMultibinder

/**
 * Guice module provisioning services for the Accio worker.
 */
object AgentWorkerModule extends TwitterModule {
  private[this] val executorUriFlag = flag[String]("executor_uri", "URI to the executor JAR")

  override def modules = Seq(FileSystemModule, OpsModule)

  override def configure(): Unit = {
    // Bind command handlers.
    val handlers = ScalaMultibinder.newSetBinder(binder, new TypeLiteral[Handler[_, _]] {})
    handlers.addBinding.to[AssignTaskHandler]
    handlers.addBinding.to[HeartbeatExecutorHandler]
    handlers.addBinding.to[KillTaskHandler]
    handlers.addBinding.to[StartExecutorHandler]
    handlers.addBinding.to[StopExecutorHandler]
    handlers.addBinding.to[StreamExecutorLogsHandler]

    // Bind configuration values.
    bind[String].annotatedWith[ExecutorUri].toInstance(executorUriFlag())
    bind[Resource].annotatedWith[ReservedResource].toInstance(Resource(0, 0, 0))
  }

  @Provides @Singleton
  def providesMasterClient(@MasterRpcDest masterAddr: String): AgentService$FinagleClient = {
    //TODO: provide an alternative for same process communication.
    val service = Thrift.client
      .withSessionQualifier.noFailFast // Because there is likely to be only one master.
      .newService(masterAddr)
    new AgentService.FinagledClient(service)
  }

  override def singletonShutdown(injector: Injector): Unit = {
    injector.instance[WorkerLifecycle].unregister()
    injector.instance[LostExecutorsObserver].kill()
    injector.instance[AgentService$FinagleClient].service.close()
  }

  override def singletonStartup(injector: Injector): Unit = {
    injector.instance[WorkerLifecycle].register()
    val observer = injector.instance[LostExecutorsObserver]
    FuturePool.unboundedPool(observer.run())
  }
}