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
import com.twitter.util.StorageUnit
import fr.cnrs.liris.accio.api.thrift.Resource
import fr.cnrs.liris.accio.config.DataDir
import fr.cnrs.liris.accio.scheduler.Scheduler
import fr.cnrs.liris.accio.scheduler.local.LocalScheduler

/**
 * Guice module provisioning the scheduler service.
 */
object SchedulerModule extends TwitterModule {
  private[this] val typeFlag = flag("scheduler.type", "local", "Scheduler type")

  // Common configuration.
  private[this] val executorUriFlag = flag[String]("executor_uri", "URI to the executor")
  private[this] val executorArgsFlag = flag("executor_args", "", "Additional arguments to the executor")

  // Local scheduler configuration.
  private[this] val forceSchedulingFlag = flag("scheduler.local.force_scheduling", false, "Whether to force the scheduling of too large tasks")
  private[this] val reservedCpuFlag = flag("scheduler.local.reserved_cpu", 0d, "Amount of CPU that is not available for scheduling")
  private[this] val reservedRamFlag = flag("scheduler.local.reserved_ram", StorageUnit.zero, "Amount of RAM that is not available for scheduling")
  private[this] val reservedDiskFlag = flag("scheduler.local.reserved_disk", StorageUnit.zero, "Disk space that is not available for scheduling")

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
    @DataDir dataDir: Path)
    extends Provider[Scheduler] {

    override def get(): Scheduler = {
      val reservedResources = Resource(
        cpu = reservedCpuFlag(),
        ramMb = reservedRamFlag().inMegabytes,
        diskMb = reservedDiskFlag().inMegabytes)
      new LocalScheduler(
        statsReceiver,
        eventBus,
        reservedResources,
        executorUriFlag(),
        executorArgs,
        forceSchedulingFlag(),
        dataDir)
    }
  }

  private def executorArgs = {
    executorArgsFlag.get
      .map(_.split(" ").map(_.trim).filter(_.nonEmpty).toSeq)
      .getOrElse(Seq.empty)
  }

}