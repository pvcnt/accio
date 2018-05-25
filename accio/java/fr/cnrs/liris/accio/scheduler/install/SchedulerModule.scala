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

package fr.cnrs.liris.accio.scheduler.install

import java.nio.file.{Files, Paths}

import com.google.inject.{Provides, Singleton}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.{Await, StorageUnit}
import fr.cnrs.liris.accio.scheduler.Scheduler
import fr.cnrs.liris.accio.scheduler.local.LocalScheduler

/**
 * Guice module provisioning the scheduler service.
 */
object SchedulerModule extends TwitterModule {
  private[this] val typeFlag = flag("scheduler", "local", "Scheduler type")

  private[this] val forceSchedulingFlag = flag("scheduler.force_scheduling", false, "Whether to force the scheduling of too large tasks")
  private[this] val dataDirFlag = flag[String]("scheduler.datadir", "Path where to store scheduler data")
  private[this] val reservedCpusFlag = flag("scheduler.reserved_cpus", 0, "Number of cores that are not available for scheduling")
  private[this] val reservedRamFlag = flag("scheduler.reserved_ram", StorageUnit.zero, "Amount of RAM that is not available for scheduling")
  private[this] val reservedDiskFlag = flag("scheduler.reserved_disk", StorageUnit.zero, "Disk space that is not available for scheduling")

  override def configure(): Unit = {
    typeFlag() match {
      case "local" => bind[Scheduler].to[LocalScheduler]
      case invalid => throw new IllegalArgumentException(s"Unknown scheduler type: $invalid")
    }
  }

  override def singletonShutdown(injector: Injector): Unit = {
    Await.ready(injector.instance[Scheduler].close())
  }

  @Provides
  @Singleton
  def providesLocalScheduler(statsReceiver: StatsReceiver): LocalScheduler = {
    val reservedResources = Map(
      "cpus" -> reservedCpusFlag().toLong,
      "ramMb" -> reservedRamFlag().inMegabytes,
      "diskGb" -> reservedDiskFlag().inGigabytes)
    val dataDir = dataDirFlag.get
      .map(Paths.get(_))
      .getOrElse(Files.createTempDirectory("accio-scheduler-"))
    new LocalScheduler(statsReceiver, reservedResources, forceSchedulingFlag(), dataDir)
  }
}