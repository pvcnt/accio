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

package fr.cnrs.liris.accio.discovery

import java.nio.file.Paths

import com.google.inject.Provider
import com.twitter.conversions.time._
import com.twitter.inject.TwitterModule

object DiscoveryModule extends TwitterModule {
  // Local discovery.
  private[this] val localRoot = flag[String]("discovery.local.root", "Directory in which to look for operator binaries")
  private[this] val localFilter = flag[String]("discovery.local.filter", "Filter to select files")

  // File discovery.
  private[this] val fileRoot = flag[String]("discovery.file.root", "Directory in which to look for definitions of operators")
  private[this] val fileFrequency = flag("discovery.file.frequency", 1.minute, "Frequency at which to check for updates")
  private[this] val fileFilter = flag[String]("discovery.file.filter", "Filter to select files")

  def args: Seq[String] = {
    flags.filter(_.isDefined).flatMap(flag => Seq(s"-${flag.name}", flag.apply().toString))
  }

  override def configure(): Unit = {
    // As there can be only one operator discovery configured, the order in which they are
    // instantiated, in case multiple flags are defined, is hard-coded.
    if (localRoot.isDefined) {
      bind[OpDiscovery].toProvider[LocalOpRegistryProvider]
    } else if (fileRoot.isDefined) {
      bind[OpDiscovery].toProvider[FileOpRegistryProvider]
    } else {
      bind[OpDiscovery].toInstance(NullOpDiscovery)
    }
  }

  private class FileOpRegistryProvider extends Provider[FileOpDiscovery] {
    override def get(): FileOpDiscovery = {
      new FileOpDiscovery(Paths.get(fileRoot()), fileFrequency(), fileFilter.get)
    }
  }

  private class LocalOpRegistryProvider extends Provider[LocalOpDiscovery] {
    override def get(): LocalOpDiscovery = {
      new LocalOpDiscovery(Paths.get(localRoot()), localFilter.get)
    }
  }

}
