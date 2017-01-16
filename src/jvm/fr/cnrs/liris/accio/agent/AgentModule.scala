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

import java.nio.file.{Path, Paths}

import com.google.inject.{Injector, Module, Provides, TypeLiteral}
import com.twitter.inject.TwitterModule
import com.twitter.util.Duration
import fr.cnrs.liris.accio.core.api.Operator
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.infra.downloader.GetterDownloaderModule
import fr.cnrs.liris.accio.core.infra.scheduler.local.{LocalSchedulerConfig, LocalSchedulerModule}
import fr.cnrs.liris.accio.core.infra.statemgr.local.{LocalStateMgrConfig, LocalStateMgrModule}
import fr.cnrs.liris.accio.core.infra.statemgr.zookeeper.{ZookeeperStateMgrConfig, ZookeeperStateMgrModule}
import fr.cnrs.liris.accio.core.infra.storage.elastic.{ElasticStorageConfig, ElasticStorageModule}
import fr.cnrs.liris.accio.core.infra.storage.local.{LocalStorageConfig, LocalStorageModule}
import fr.cnrs.liris.accio.core.infra.uploader.local.{LocalUploaderConfig, LocalUploaderModule}
import fr.cnrs.liris.accio.core.service._
import net.codingwell.scalaguice.ScalaMultibinder

import scala.collection.mutable

object AgentModule extends TwitterModule {
  private[this] val _workDir = flag("workdir", "/var/lib/accio-agent", "Working directory")

  private[this] val _schedulerType = flag("scheduler.type", "local", "Scheduler type")
  private[this] val _executorUri = flag[String]("scheduler.executor_uri", "URI to the executor JAR")
  private[this] val _javaHome = flag[String]("scheduler.local.java_home", "Path to JRE when launching the executor")

  private[this] val _stateMgrType = flag("statemgr.type", "local", "State manager type")
  private[this] val _zkAddr = flag("statemgr.zk.addr", "127.0.0.1:2181", "Address to Zookeeper cluster")
  private[this] val _zkRootPath = flag("statemgr.zk.path", "/accio", "Root path in Zookeeper")
  private[this] val _zkSessionTimeout = flag("statemgr.zk.session_timeout", Duration.fromSeconds(60), "Zookeeper session timeout")
  private[this] val _zkConnTimeout = flag("statemgr.zk.conn_timeout", Duration.fromSeconds(15), "Zookeeper connection timeout")

  private[this] val _uploaderType = flag("uploader.type", "local", "Uploader type")

  private[this] val _storageType = flag("storage.type", "local", "Storage type")
  private[this] val _esAddr = flag("storage.es.addr", "127.0.0.1:9300", "Address to Elasticsearch cluster")
  private[this] val _esSniff = flag("storage.es.sniff", true, "Whether to 'sniff' Elasticsearch cluster members")
  private[this] val _esPrefix = flag("storage.es.prefix", "accio__", "Prefix of Elasticsearch indices")
  private[this] val _esQueryTimeout = flag("storage.es.query_timeout", Duration.fromSeconds(15), "Elasticsearch query timeout")

  private def workDir: Path = Paths.get(_workDir())

  protected override def configure(): Unit = {
    val modules = mutable.ListBuffer.empty[Module]
    modules += GetterDownloaderModule

    // Bind correct modules.
    _schedulerType() match {
      case "local" => modules += new LocalSchedulerModule
      case unknown => throw new IllegalArgumentException(s"Unknown scheduler type: $unknown")
    }
    _stateMgrType() match {
      case "local" => modules += new LocalStateMgrModule
      case "zk" => modules += new ZookeeperStateMgrModule
      case unknown => throw new IllegalArgumentException(s"Unknown state manager type: $unknown")
    }
    _uploaderType() match {
      case "local" => modules += new LocalUploaderModule
      case unknown => throw new IllegalArgumentException(s"Unknown uploader type: $unknown")
    }
    _storageType() match {
      case "local" => modules += new LocalStorageModule
      case "es" => modules += new ElasticStorageModule
      case unknown => throw new IllegalArgumentException(s"Unknown storage type: $unknown")
    }

    // Initialize modules with configuration.
    val localUploaderConfig = LocalUploaderConfig(workDir.resolve("uploads"))
    val configurator = Configurator(
      LocalSchedulerConfig(workDir.resolve("sandbox"), "127.0.0.1:9999", _executorUri(), _javaHome.get, _uploaderType(), Map("path" -> localUploaderConfig.rootDir.toString)),
      LocalStateMgrConfig(workDir.resolve("state")),
      ZookeeperStateMgrConfig(_zkAddr(), _zkRootPath(), _zkSessionTimeout(), _zkConnTimeout()),
      LocalStorageConfig(workDir.resolve("storage")),
      ElasticStorageConfig(_esAddr(), _esSniff(), _esPrefix(), _esQueryTimeout()),
      localUploaderConfig
    )
    configurator.initialize(modules: _*)

    modules.foreach(install)

    ScalaMultibinder.newSetBinder(binder, new TypeLiteral[Class[_ <: Operator[_, _]]] {})
    bind[OpMetaReader].to[ReflectOpMetaReader]
    bind[OpRegistry].to[RuntimeOpRegistry]
  }

  @Provides
  def providesOpFactory(opRegistry: RuntimeOpRegistry, injector: Injector): OpFactory = {
    new OpFactory(opRegistry, injector: Injector)
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
}
