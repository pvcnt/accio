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

import java.net.InetAddress
import java.nio.file.{Path, Paths}
import java.util.concurrent.{ExecutorService, Executors}

import com.google.common.eventbus.{AsyncEventBus, EventBus}
import com.google.inject.{Module, Provides, Singleton, TypeLiteral}
import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.inject.TwitterModule
import com.twitter.util.{Duration, FuturePool}
import fr.cnrs.liris.accio.agent.config._
import fr.cnrs.liris.accio.framework.discovery.inject.DiscoveryModule
import fr.cnrs.liris.accio.framework.filesystem.inject.FileSystemModule
import fr.cnrs.liris.accio.framework.service._
import fr.cnrs.liris.accio.framework.util.{WorkDir, WorkerPool}
import fr.cnrs.liris.accio.runtime.commandbus.Handler
import fr.cnrs.liris.common.util.Platform
import net.codingwell.scalaguice.ScalaMultibinder

import scala.collection.mutable

/**
 * Guice module provisioning services for the Accio agent.
 */
object AgentModule extends TwitterModule {
  private[this] val masterFlag = flag("master", false, "Whether this agent is a master")
  private[this] val workerFlag = flag("worker", false, "Whether this agent is a worker")

  private[this] val bindFlag = flag("bind", "0.0.0.0", "Address on which to bound network interfaces")
  private[this] val masterAddrFlag = flag[String]("addr", "Address of the Accio master")
  private[this] val workdirFlag = flag("workdir", "/var/lib/accio-agent", "Directory where to store local files")
  private[this] val clusterNameFlag = flag("cluster_name", "default", "Cluster name")
  private[this] val agentNameFlag = flag[String]("agent_name", "Agent name")

  override def modules: Seq[Module] = {
    if (masterFlag.isDefined || workerFlag.isDefined) {
      val active = mutable.ListBuffer[Module](DiscoveryModule)
      if (masterFlag()) {
        active += AgentMasterModule
      }
      if (workerFlag()) {
        active += AgentWorkerModule
      }
      active.toList
    } else {
      // This only happen when this method is called the first time, during App's initialization.
      // We provide the entire list of all modules, making all flags available.
      Seq(AgentMasterModule, AgentWorkerModule, DiscoveryModule)
    }
  }

  protected override def configure(): Unit = {
    // Create an empty set of command handlers, in case nothing else is bound.
    ScalaMultibinder.newSetBinder(binder, new TypeLiteral[Handler[_, _]] {})

    // Bind remaining implementations.
    bind[OpRegistry].to[RuntimeOpRegistry]

    // Bind configuration values.
    bind[String].annotatedWith[ClusterName].toInstance(clusterNameFlag())
    // Default is only valid with a same-host master.
    bind[String].annotatedWith[MasterRpcDest].toInstance(masterAddrFlag.getWithDefault.getOrElse(bindFlag() + ":9999"))
    bind[String].annotatedWith[AgentName].toInstance(agentNameFlag.get.getOrElse(Platform.hostname))
    bind(new TypeLiteral[Seq[String]] {}).annotatedWith(classOf[ExecutorArgs]).toInstance {
      FileSystemModule.executorPassthroughFlags.flatMap { flag =>
        flag.getWithDefault.map(v => s"-${flag.name}=$v").toSeq
      }
    }
    bind[Path].annotatedWith[WorkDir].toInstance(Paths.get(workdirFlag()))
    bind[Duration].annotatedWith[WorkerTimeout].toInstance(Duration.fromSeconds(60))
    bind[Duration].annotatedWith[ExecutorTimeout].toInstance(Duration.fromSeconds(60))
  }

  /*@Singleton @Provides @MasterRpcDest
  def providesMasterRpcDest(@Flag("thrift.port") port: String): String = {
    val pos = port.indexOf(":")
    if (pos == -1) {
      s"$hostname:$port"
    } else if (pos == 0) {
      s"$hostname$port"
    } else {
      port
    }
  }*/

  @Provides @Singleton @WorkerPool
  def providesWorkerPool(@WorkerPool executor: ExecutorService): FuturePool = {
    FuturePool.interruptible(executor)
  }

  @Provides @Singleton @WorkerPool
  def providesExecutorService: ExecutorService = {
    Executors.newCachedThreadPool(new NamedPoolThreadFactory("agent/worker"))
  }

  @Provides @Singleton
  def providesEventBus(@WorkerPool executor: ExecutorService): EventBus = {
    new AsyncEventBus(executor)
  }
}