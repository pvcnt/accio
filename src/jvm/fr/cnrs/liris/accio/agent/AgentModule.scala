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

import java.nio.file.{Path, Paths}

import com.google.inject.{Module, Provides, TypeLiteral}
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.Duration
import fr.cnrs.liris.accio.core.api.Operator
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.infra.downloader.GetterDownloaderModule
import fr.cnrs.liris.accio.core.infra.scheduler.local.{LocalScheduler, LocalSchedulerConfig, LocalSchedulerModule}
import fr.cnrs.liris.accio.core.infra.statemgr.local.{LocalStateMgrConfig, LocalStateMgrModule}
import fr.cnrs.liris.accio.core.infra.statemgr.zookeeper.{ZookeeperStateMgrConfig, ZookeeperStateMgrModule}
import fr.cnrs.liris.accio.core.infra.storage.elastic.{ElasticStorageConfig, ElasticStorageModule}
import fr.cnrs.liris.accio.core.infra.storage.local.{LocalStorageConfig, LocalStorageModule}
import fr.cnrs.liris.accio.core.infra.uploader.local.{LocalUploaderConfig, LocalUploaderModule}
import fr.cnrs.liris.accio.core.service._
import net.codingwell.scalaguice.ScalaMultibinder

import scala.collection.mutable

object AgentModule extends TwitterModule {
  private[this] val advertiseFlag = flag("advertise", "127.0.0.1:9999", "Address to advertise to executors")
  private[this] val workDirFlag = flag("workdir", "/var/lib/accio-agent", "Working directory")

  private[this] val schedulerFlag = flag("scheduler.type", "local", "Scheduler type")
  private[this] val executorUriFlag = flag[String]("scheduler.executor_uri", "URI to the executor JAR")
  private[this] val javaHomeFlag = flag[String]("scheduler.local.java_home", "Path to JRE when launching the executor")

  private[this] val stateMgrFlag = flag("statemgr.type", "local", "State manager type")
  private[this] val zkStateMgrAddrFlag = flag("statemgr.zk.addr", "127.0.0.1:2181", "Address to Zookeeper cluster")
  private[this] val zkStateMgrPathFlag = flag("statemgr.zk.path", "/accio", "Root path in Zookeeper")
  private[this] val zkStateMgrSessionTimeoutFlag = flag("statemgr.zk.session_timeout", Duration.fromSeconds(60), "Zookeeper session timeout")
  private[this] val zkStateMgrConnTimeoutFlag = flag("statemgr.zk.conn_timeout", Duration.fromSeconds(15), "Zookeeper connection timeout")

  private[this] val uploaderFlag = flag("uploader.type", "local", "Uploader type")
  private[this] val localUploaderPathFlag = flag[String]("uploader.local.path", "Local uploader path")

  private[this] val storageFlag = flag("storage.type", "local", "Storage type")
  private[this] val esStorageAddrFlag = flag("storage.es.addr", "127.0.0.1:9300", "Address to Elasticsearch cluster")
  private[this] val esStoragePrefixFlag = flag("storage.es.prefix", "accio__", "Prefix of Elasticsearch indices")
  private[this] val esStorageQueryTimeoutFlag = flag("storage.es.query_timeout", Duration.fromSeconds(15), "Elasticsearch query timeout")

  private[this] val executorPassthroughFlags = Seq(uploaderFlag, localUploaderPathFlag)

  private def workDir: Path = Paths.get(workDirFlag())

  protected override def configure(): Unit = {
    val modules = mutable.ListBuffer.empty[Module]
    modules += GetterDownloaderModule

    // Install appropriate modules.
    stateMgrFlag() match {
      case "local" => modules += new LocalStateMgrModule(LocalStateMgrConfig(workDir.resolve("state")))
      case "zk" =>
        val config = ZookeeperStateMgrConfig(zkStateMgrAddrFlag(), zkStateMgrPathFlag(), zkStateMgrSessionTimeoutFlag(), zkStateMgrConnTimeoutFlag())
        modules += new ZookeeperStateMgrModule(config)
      case unknown => throw new IllegalArgumentException(s"Unknown state manager type: $unknown")
    }
    uploaderFlag() match {
      case "local" => modules += new LocalUploaderModule(LocalUploaderConfig(Paths.get(localUploaderPathFlag())))
      case unknown => throw new IllegalArgumentException(s"Unknown uploader type: $unknown")
    }
    storageFlag() match {
      case "local" => modules += new LocalStorageModule(LocalStorageConfig(workDir.resolve("storage")))
      case "es" =>
        val config = ElasticStorageConfig(esStorageAddrFlag(), esStoragePrefixFlag(), esStorageQueryTimeoutFlag())
        modules += new ElasticStorageModule(config)
      case unknown => throw new IllegalArgumentException(s"Unknown storage type: $unknown")
    }
    schedulerFlag() match {
      case "local" =>
        val executorArgs = executorPassthroughFlags.flatMap { flag =>
          flag.getWithDefault.map(v => Seq(s"-${flag.name}", v)).getOrElse(Seq.empty[String])
        }
        val config = LocalSchedulerConfig(workDir.resolve("sandbox"), advertiseFlag(), executorUriFlag(), javaHomeFlag.get, executorArgs)
        modules += new LocalSchedulerModule(config)
      case unknown => throw new IllegalArgumentException(s"Unknown scheduler type: $unknown")
    }
    modules.foreach(install)

    // Create an empty set of operators, in case nothing else is bound.
    ScalaMultibinder.newSetBinder(binder, new TypeLiteral[Class[_ <: Operator[_, _]]] {})

    // Bind remaining implementations.
    bind[OpMetaReader].to[ReflectOpMetaReader]
    bind[OpRegistry].to[RuntimeOpRegistry]
  }

  @Provides
  def providesOpFactory(opRegistry: RuntimeOpRegistry, injector: com.google.inject.Injector): OpFactory = {
    new OpFactory(opRegistry, injector)
  }

  @Provides
  def providesSchedulerService(scheduler: Scheduler, stateManager: StateManager): SchedulerService = {
    new SchedulerService(scheduler: Scheduler, stateManager: StateManager)
  }

  @Provides
  def providesGraphFactory(opRegistry: OpRegistry): GraphFactory = {
    new GraphFactory(opRegistry)
  }

  @Provides
  def providesWorkflowFactory(graphFactory: GraphFactory, opRegistry: OpRegistry): WorkflowFactory = {
    new WorkflowFactory(graphFactory, opRegistry)
  }

  @Provides
  def providesRunFactory(workflowRepository: WorkflowRepository): RunFactory = {
    new RunFactory(workflowRepository)
  }

  override def singletonShutdown(injector: Injector): Unit = {
    schedulerFlag() match {
      case "local" => injector.instance[LocalScheduler].stop()
    }
  }
}
