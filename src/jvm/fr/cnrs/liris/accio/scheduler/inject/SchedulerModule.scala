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

import com.google.inject.Module
import com.twitter.inject.{Injector, TwitterModule}
import fr.cnrs.liris.accio.scheduler.Scheduler
import fr.cnrs.liris.accio.scheduler.standalone._

/**
 * Guice module provisioning the scheduler service.
 */
object SchedulerModule extends TwitterModule {
  private[this] val typeFlag = flag("scheduler.type", "standalone", "Scheduler type")

  override def modules: Seq[Module] =
    typeFlag.get match {
      case Some("standalone") => Seq(StandaloneSchedulerModule)
      case Some(invalid) => throw new IllegalArgumentException(s"Invalid scheduler type: $invalid")
      case None =>
        // This only happen when this method is called the first time, during App's initialization.
        // We provide the entire list of all modules, making all flags available.
        Seq(StandaloneSchedulerModule)
    }

  override def singletonShutdown(injector: Injector): Unit = {
    injector.instance[Scheduler].close()
  }
}