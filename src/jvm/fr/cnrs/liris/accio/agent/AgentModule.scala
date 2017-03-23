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

import java.net.InetAddress
import java.nio.file.{Path, Paths}
import java.util.UUID
import java.util.concurrent.Executors

import com.google.inject.{Provides, Singleton, TypeLiteral}
import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.finagle.Thrift
import com.twitter.finagle.service._
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.{Duration, FuturePool}
import fr.cnrs.liris.accio.agent.commandbus.Handler
import fr.cnrs.liris.accio.agent.config._
import fr.cnrs.liris.accio.agent.handler.inject.{MasterHandlerModule, WorkerHandlerModule}
import fr.cnrs.liris.accio.core.api.Operator
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.filesystem.inject.FileSystemModule
import fr.cnrs.liris.accio.core.finagle.AccioResponseClassifier
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.accio.core.util.{ClusterName, WorkDir, WorkerPool}
import net.codingwell.scalaguice.ScalaMultibinder

import scala.util.Try

/**
 * Guice module provisioning services for the Accio agent.
 */
object AgentModule extends TwitterModule {
  private[this] val bindFlag = flag("bind", "0.0.0.0", "Address on which to bound network interfaces")
  private[this] val masterAddrFlag = flag[String]("addr", "Address of the Accio master")
  private[this] val executorUriFlag = flag[String]("executor_uri", "URI to the executor JAR")
  private[this] val workdirFlag = flag("workdir", "/var/lib/accio-agent", "Directory where to store local files")
  private[this] val clusterNameFlag = flag("cluster_name", "default", "Cluster name")
  private[this] val agentNameFlag = flag[String]("agent_name", "Agent name")
  private[agent] val masterFlag = flag("master", false, "Whether this agent is a master")
  private[agent] val workerFlag = flag("worker", false, "Whether this agent is a worker")

  protected override def configure(): Unit = {
    // Create an empty set of operators, in case nothing else is bound.
    ScalaMultibinder.newSetBinder(binder, new TypeLiteral[Class[_ <: Operator[_, _]]] {})

    // Bind command handlers.
    ScalaMultibinder.newSetBinder(binder, new TypeLiteral[Handler[_, _]] {})
    if (masterFlag()) {
      install(MasterHandlerModule)
    }
    if (workerFlag()) {
      install(WorkerHandlerModule)
    }

    // Bind remaining implementations.
    bind[OpMetaReader].to[ReflectOpMetaReader]
    bind[OpRegistry].to[RuntimeOpRegistry]

    // Bind configuration values.
    bind[String].annotatedWith[ClusterName].toInstance(clusterNameFlag())
    bind[String].annotatedWith[MasterRpcDest].toInstance(if (masterFlag()) masterAddrFlag.getWithDefault.getOrElse(bindFlag() + ":9999") else masterAddrFlag())
    // Executors are launched on the same host, without isolation, we can use directly the bind address.
    bind[String].annotatedWith[WorkerRpcDest].toInstance(s"inet!${bindFlag()}:9999")
    bind[String].annotatedWith[AgentName].toInstance(agentNameFlag.get.getOrElse(Try(InetAddress.getLocalHost.getHostName).getOrElse(UUID.randomUUID().toString)))
    bind[String].annotatedWith[ExecutorUri].toInstance(executorUriFlag())
    bind(new TypeLiteral[Seq[String]] {}).annotatedWith(classOf[ExecutorArgs]).toInstance {
      FileSystemModule.executorPassthroughFlags.flatMap { flag =>
        flag.getWithDefault.map(v => s"-${flag.name}=$v").toSeq
      }
    }
    bind[Resource].annotatedWith[ReservedResource].toInstance(Resource(0, 0, 0))
    bind[Path].annotatedWith[WorkDir].toInstance(Paths.get(workdirFlag()))
    bind[Duration].annotatedWith[WorkerTimeout].toInstance(Duration.fromSeconds(60))
    bind[Duration].annotatedWith[ExecutorTimeout].toInstance(Duration.fromSeconds(60))
  }

  @Provides @Singleton @WorkerPool
  def providesWorkerPool: FuturePool = {
    val executorService = Executors.newCachedThreadPool(new NamedPoolThreadFactory("agent/worker"))
    FuturePool.interruptible(executorService)
  }

  @Provides @Singleton
  def providesMasterClient(@MasterRpcDest masterAddr: String): AgentService$FinagleClient = {
    //TODO: provide an alternative for same process communication.
    val service = Thrift.client
      .withRetryBudget(RetryBudget.Infinite)
      .withRetryBackoff(Backoff.const(Duration.fromSeconds(15)))
      .withSessionQualifier.noFailFast // Because there is likely to be only one master.
      .withResponseClassifier(AccioResponseClassifier.Default)
      .newService(masterAddr)
    new AgentService.FinagledClient(service)
  }

  override def singletonShutdown(injector: Injector): Unit = {
    injector.instance[AgentService$FinagleClient].service.close()
  }
}