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

package fr.cnrs.liris.accio.executor

import java.util.concurrent.Executors

import com.google.inject._
import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.finagle.Thrift
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.{ExecutorServiceFuturePool, FuturePool}
import fr.cnrs.liris.accio.agent.{AgentService, AgentService$FinagleClient}
import fr.cnrs.liris.accio.framework.discovery.inject.DiscoveryModule
import fr.cnrs.liris.accio.framework.util.WorkerPool

/**
 * Guice module providing executor-specific bindings.
 */
object ExecutorModule extends TwitterModule {
  private[this] val addrFlag = flag[String]("addr", "Address of the Accio worker")

  override def modules = Seq(DiscoveryModule)

  @Singleton
  @Provides
  @WorkerPool
  def providesWorkerPool: FuturePool = {
    val executorService = Executors.newCachedThreadPool(new NamedPoolThreadFactory("executor"))
    FuturePool.interruptible(executorService)
  }

  @Singleton
  @Provides
  def providesClient: AgentService$FinagleClient = {
    // - We communicate directly with our local worker (and *not* with the master).
    // - If the executor is still alive, then the worker is still alive (because the executor is a subprocess of
    //   the worker's process). So we retry indefinitely, w.r.t. our domain-aware response classifier (e.g.,
    //   avoiding to retry on InvalidTaskException's).
    // - We do not want to fail-fast, because there will always be only one reachable host.
    val service = Thrift.client
      .withSessionQualifier.noFailFast
      .newService(addrFlag())
    new AgentService.FinagledClient(service)
  }

  override def singletonShutdown(injector: Injector): Unit = {
    injector.instance[AgentService$FinagleClient].service.close()
    injector.instance[FuturePool, WorkerPool] match {
      case p: ExecutorServiceFuturePool => p.executor.shutdownNow()
    }
  }
}