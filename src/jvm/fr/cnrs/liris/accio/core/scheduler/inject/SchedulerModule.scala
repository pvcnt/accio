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

package fr.cnrs.liris.accio.core.scheduler.inject

import java.nio.file.Paths

import com.twitter.inject.{Injector, TwitterModule}
import fr.cnrs.liris.accio.core.filesystem.inject.FileSystemModule
import fr.cnrs.liris.accio.core.scheduler.{LocalScheduler, LocalSchedulerConfig, Scheduler}

/**
 * Guice module provisioning the scheduler service.
 */
object SchedulerModule extends TwitterModule {
  private[this] val advertiseFlag = flag("advertise", "127.0.0.1:9999", "Address to advertise to executors")
  private[this] val executorUriFlag = flag[String]("scheduler.executor_uri", "URI to the executor JAR")
  private[this] val javaHomeFlag = flag[String]("scheduler.java_home", "Path to JRE when launching the executor")
  private[this] val localPathFlag = flag[String]("scheduler.local.path", "Directory where to store sandboxes")

  protected override def configure(): Unit = {
    val executorArgs = FileSystemModule.executorPassthroughFlags.flatMap { flag =>
      flag.getWithDefault.map(v => s"-${flag.name}=$v").toSeq
    }
    bind[Scheduler].to[LocalScheduler]
    val config = LocalSchedulerConfig(Paths.get(localPathFlag()), advertiseFlag(), executorUriFlag(), javaHomeFlag.get, executorArgs)
    bind[LocalSchedulerConfig].toInstance(config)
  }

  override def singletonShutdown(injector: Injector): Unit = {
    injector.instance[Scheduler].close()
  }
}