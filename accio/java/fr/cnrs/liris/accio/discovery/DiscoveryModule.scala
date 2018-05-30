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

import com.google.inject.{Provider, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.{Await, Duration}

/**
 * Guice module providing an operator discovery and registry.
 */
object DiscoveryModule extends TwitterModule {
  // File discovery.
  private[this] val fileRoot = flag[String](
    "discovery.file.path",
    "Directory in which to look for operator libraries. Only the files directly under " +
      "that directory will be considered (i.e., sub-directories are ignored).")
  private[this] val fileFrequency = flag[Duration](
    "discovery.file.frequency",
    "Frequency at which to check for file changes. 0 means disabled.")
  private[this] val fileFilter = flag[String](
    "discovery.file.filter",
    "Regex used to filter the files. If set, only the files whose name matches this regex " +
      "will be considered.")

  /**
   * Return the list of command-line arguments that define the configuration for this module.
   */
  def args: Seq[String] = {
    flags.filter(_.isDefined).flatMap(flag => Seq(s"-${flag.name}", flag.apply().toString))
  }

  override protected def configure(): Unit = {
    // As there can be only one operator discovery configured, the order in which they are
    // instantiated, in case multiple flags are defined, is hard-coded.
    if (fileRoot.isDefined) {
      bind[OpDiscovery].toProvider[FileOpRegistryProvider].in[Singleton]
    } else {
      bind[OpDiscovery].toInstance(NullOpDiscovery)
    }
  }

  override def singletonShutdown(injector: Injector): Unit = {
    Await.result(injector.instance[OpDiscovery].close())
  }

  private class FileOpRegistryProvider extends Provider[FileOpDiscovery] {
    override def get(): FileOpDiscovery = {
      new FileOpDiscovery(Paths.get(fileRoot()), fileFrequency.get.getOrElse(Duration.Zero), fileFilter.get)
    }
  }

}
