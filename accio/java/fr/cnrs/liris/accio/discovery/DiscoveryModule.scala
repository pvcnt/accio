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

import com.google.inject.{Provides, Singleton}
import com.twitter.conversions.time._
import com.twitter.inject.TwitterModule

object DiscoveryModule extends TwitterModule {
  private[this] val localRoot = flag[String]("discovery.local.root", "Directory in which to look for operator binaries")
  private[this] val localFilter = flag[String]("discovery.local.filter", "Filters to select files")
  private[this] val fileRoot = flag[String]("discovery.file.root", "Directory in which to look for definitions of operators")
  private[this] val fileFrequency = flag("discovery.file.frequency", 1.minute, "Frequency at which to check for updates")

  def forwardableArgs: Seq[String] = {
    flags.filter(_.isDefined).map(flag => s"-${flag.name}=${flag()}")
  }

  override def configure(): Unit = {
    if (localRoot.isDefined) {
      bind[OpRegistry].to[LocalOpRegistry]
    } else if (fileRoot.isDefined) {
      bind[OpRegistry].to[FileOpRegistry]
    } else {
      bind[OpRegistry].toInstance(NullOpRegistry)
    }
  }

  @Provides
  @Singleton
  def providesFileOpRegistry: FileOpRegistry = {
    new FileOpRegistry(Paths.get(fileRoot()), fileFrequency())
  }

  @Provides
  @Singleton
  def providesLocalOpRegistry: LocalOpRegistry = {
    new LocalOpRegistry(Paths.get(localRoot()), localFilter.get)
  }
}
