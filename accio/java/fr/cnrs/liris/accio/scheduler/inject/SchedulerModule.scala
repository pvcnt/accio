/*
 * Accio is a platform to launch computer science experiments.
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

package fr.cnrs.liris.accio.scheduler.inject

import java.nio.file.Path

import com.google.common.eventbus.EventBus
import com.google.inject.{Inject, Provider, Singleton}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.{Injector, TwitterModule}
import fr.cnrs.liris.accio.api.thrift.Resource
import fr.cnrs.liris.accio.config.{DataDir, ExecutorArgs, ExecutorUri, ReservedResource}
import fr.cnrs.liris.accio.scheduler.Scheduler
import fr.cnrs.liris.accio.scheduler.local.LocalScheduler

/**
 * Guice module provisioning the scheduler service.
 */
object SchedulerModule extends TwitterModule {
  private[this] val typeFlag = flag("scheduler.type", "local", "Scheduler type")

  override def configure(): Unit = {
    typeFlag() match {
      case "local" => bind[Scheduler].toProvider[LocalSchedulerProvider].in[Singleton]
      case invalid => throw new IllegalArgumentException(s"Unknown scheduler type: $invalid")
    }
  }

  override def singletonStartup(injector: Injector): Unit = {
    injector.instance[Scheduler].startUp()
  }

  override def singletonShutdown(injector: Injector): Unit = {
    injector.instance[Scheduler].shutDown()
  }

  private class LocalSchedulerProvider @Inject()(
    statsReceiver: StatsReceiver,
    eventBus: EventBus,
    @ReservedResource reserved: Resource,
    @ExecutorUri executorUri: String,
    @ExecutorArgs executorArgs: Seq[String],
    @DataDir dataDir: Path)
    extends Provider[Scheduler] {

    override def get(): Scheduler = {
      new LocalScheduler(statsReceiver, eventBus, reserved, executorUri, executorArgs, dataDir.resolve("tasks"))
    }
  }

}