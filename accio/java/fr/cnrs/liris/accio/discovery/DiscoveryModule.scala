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
  private[this] val dirFile = flag[String]("discovery.dir.file", "Directory in which to look for definitions of operators")
  private[this] val dirFrequency = flag("discovery.dir.frequency", 1.minute, "Frequency at which to check for updates")

  override def configure(): Unit = {
    if (dirFile.isDefined) {
      bind[OpRegistry].to[DirectoryOpRegistry]
    }
  }

  @Provides
  @Singleton
  def providesDirectoryOpRegistry: DirectoryOpRegistry = {
    new DirectoryOpRegistry(Paths.get(dirFile()), dirFrequency())
  }
}
