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

import com.google.inject.{Provides, Singleton, TypeLiteral}
import com.twitter.finagle.Thrift
import com.twitter.finagle.param.HighResTimer
import com.twitter.finagle.service.{Backoff, RetryFilter}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.ThriftClientRequest
import com.twitter.inject.annotations.Flag
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util._
import fr.cnrs.liris.accio.agent.config._
import fr.cnrs.liris.accio.agent.handler._
import fr.cnrs.liris.accio.framework.api.thrift.Resource
import fr.cnrs.liris.accio.framework.filesystem.inject.FileSystemModule
import fr.cnrs.liris.accio.runtime.commandbus.Handler
import fr.cnrs.liris.accio.runtime.finagle.RetryPolicies
import fr.cnrs.liris.common.util.Platform
import net.codingwell.scalaguice.ScalaMultibinder

/**
 * Guice module provisioning services for the Accio worker.
 */
object AgentWorkerModule extends TwitterModule {
  private[this] val executorUriFlag = flag[String]("executor_uri", "URI to the executor JAR")

  override def modules = Seq(FileSystemModule)

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

  @Singleton @Provides @WorkerRpcDest
  def providesWorkerRpcDest(@Flag("thrift.port") thriftAddress: String): String = {
    val port = thriftAddress.drop(thriftAddress.indexOf(":") + 1)
    val dest = Platform.hostname + ":" + port
    logger.info(s"Advertising $dest")
    dest
  }

  @Provides @Singleton
  def providesMasterClient(@MasterRpcDest masterAddr: String, statsReceiver: StatsReceiver): AgentService$FinagleClient = {
    // We configure the client to retry constantly every 15 seconds if the master is not available.
    // We disable fail-fast module, because will often be only one master.
    val retryPolicy = RetryPolicies.onFailure[ThriftClientRequest, Array[Byte]](Backoff.const(Duration.fromSeconds(15)))
    val retryFilter = new RetryFilter(retryPolicy, HighResTimer.Default, statsReceiver)
    val service = Thrift.client
      .withSessionQualifier.noFailFast
      .filtered(retryFilter)
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