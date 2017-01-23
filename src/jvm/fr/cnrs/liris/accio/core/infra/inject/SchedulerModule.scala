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

package fr.cnrs.liris.accio.core.infra.inject

import java.nio.file.Paths

import com.twitter.inject.{Injector, TwitterModule}
import fr.cnrs.liris.accio.core.infra.scheduler.local.{LocalSchedulerConfig, LocalSchedulerModule}
import fr.cnrs.liris.accio.core.service._

/**
 * Guice module provisioning the [[Scheduler]] service.
 */
object SchedulerModule extends TwitterModule {
  private[this] val advertiseFlag = flag("advertise", "127.0.0.1:9999", "Address to advertise to executors")
  private[this] val schedulerFlag = flag("scheduler.type", "local", "Scheduler type")
  private[this] val executorUriFlag = flag[String]("scheduler.executor_uri", "URI to the executor JAR")

  // Local scheduler configuration.
  private[this] val localSchedulerPathFlag = flag[String]("scheduler.local.path", "Path where to store sandboxes")
  private[this] val localSchedulerJavaHomeFlag = flag[String]("scheduler.local.java_home", "Path to JRE when launching the executor")

  // Flags that will be forwarded "as-is" when invoking the executor.
  private[this] val executorPassthroughFlags = Seq(UploaderModule.uploaderFlag, UploaderModule.localUploaderPathFlag)

  protected override def configure(): Unit = {
    val module = schedulerFlag() match {
      case "local" =>
        val executorArgs = executorPassthroughFlags.flatMap { flag =>
          flag.getWithDefault.map(v => Seq(s"-${flag.name}", v)).getOrElse(Seq.empty[String])
        }
        val config = LocalSchedulerConfig(Paths.get(localSchedulerPathFlag()), advertiseFlag(), executorUriFlag(), localSchedulerJavaHomeFlag.get, executorArgs)
        new LocalSchedulerModule(config)
      case unknown => throw new IllegalArgumentException(s"Unknown scheduler type: $unknown")
    }
    install(module)
  }

  override def singletonShutdown(injector: Injector): Unit = {
    injector.instance[Scheduler].stop()
  }
}