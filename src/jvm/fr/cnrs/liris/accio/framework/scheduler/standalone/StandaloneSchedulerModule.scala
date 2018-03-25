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

package fr.cnrs.liris.accio.framework.scheduler.standalone

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterPrivateModule}
import fr.cnrs.liris.accio.framework.scheduler.Scheduler

/**
 * Guice module provisioning a standalone scheduler service.
 */
object StandaloneSchedulerModule extends TwitterPrivateModule {
  private[this] val fitnessCalculatorFlag = flag("scheduler.standalone.bin_packer", "cpu,ram", "Bin packing algorithms to use")

  override def configure(): Unit = {
    bind[Scheduler].to[StandaloneScheduler]
    expose[Scheduler]
  }

  @Provides @Singleton
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
    injector.instance[WorkerClientProvider].close()
  }
}