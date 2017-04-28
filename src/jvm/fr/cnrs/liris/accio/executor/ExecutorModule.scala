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
import com.twitter.finagle.param.HighResTimer
import com.twitter.finagle.service.{Backoff, RetryFilter}
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.finagle.thrift.ThriftClientRequest
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.{Duration, ExecutorServiceFuturePool, FuturePool}
import fr.cnrs.liris.accio.agent.{AgentService, AgentService$FinagleClient}
import fr.cnrs.liris.accio.framework.discovery.inject.DiscoveryModule
import fr.cnrs.liris.accio.framework.util.WorkerPool
import fr.cnrs.liris.accio.runtime.finagle.RetryPolicies

/**
 * Guice module providing executor-specific bindings.
 */
object ExecutorModule extends TwitterModule {
  private[this] val addrFlag = flag[String]("addr", "Address of the Accio worker")

  override def modules = Seq(DiscoveryModule)

  override def configure(): Unit = {
    bind[StatsReceiver].to[NullStatsReceiver]
  }

  @Singleton @Provides @WorkerPool
  def providesWorkerPool: FuturePool = {
    val executorService = Executors.newCachedThreadPool(new NamedPoolThreadFactory("executor"))
    FuturePool.interruptible(executorService)
  }

  @Singleton @Provides
  def providesClient(statsReceiver: StatsReceiver): AgentService$FinagleClient = {
    // We communicate directly with our local worker (and *not* with the master).
    // We configure the client to retry constantly every 15 seconds if the master is not available.
    // We disable fail-fast module, because will often be only one master.
    val retryPolicy = RetryPolicies.onFailure[ThriftClientRequest, Array[Byte]](Backoff.const(Duration.fromSeconds(15)))
    val retryFilter = new RetryFilter(retryPolicy, HighResTimer.Default, statsReceiver)
    val service = Thrift.client
      .withSessionQualifier.noFailFast
      .filtered(retryFilter)
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