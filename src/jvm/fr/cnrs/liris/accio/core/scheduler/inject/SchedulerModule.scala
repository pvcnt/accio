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

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import fr.cnrs.liris.accio.core.scheduler.standalone._
import fr.cnrs.liris.accio.core.scheduler.{Scheduler, WorkerClientFactory}

/**
 * Guice module provisioning the scheduler service.
 */
object SchedulerModule extends TwitterModule {
  private[this] val fitnessCalculatorFlag = flag("scheduler.bin_packer", "cpu,ram", "Bin packing algorithms to use")

  protected override def configure(): Unit = {
    bind[Scheduler].to[StandaloneScheduler]
  }

  @Provides
  @Singleton
  def providesFitnessCalculator: FitnessCalculator = {
    val types = fitnessCalculatorFlag().split(",")
    val packers = types.map {
      case "cpu" => new CpuBinPacker
      case "ram" => new RamBinPacker
      case "disk" => new DiskBinPacker
      case invalid => throw new IllegalArgumentException(s"Invalid bin packer: $invalid")
    }
    require(packers.nonEmpty, "You must specify at least one bin packing algorithm to use")
    new ComposedFitnessCalculator(packers)
  }

  override def singletonShutdown(injector: Injector): Unit = {
    injector.instance[Scheduler].close()
    injector.instance[WorkerClientFactory].close()
  }
}