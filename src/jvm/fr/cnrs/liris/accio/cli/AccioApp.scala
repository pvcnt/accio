/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.cli

import java.nio.file.Path

import com.google.inject.Guice
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.framework.FrameworkModule
import fr.cnrs.liris.accio.core.runtime.LocalRuntimeModule
import fr.cnrs.liris.common.flags._
import fr.cnrs.liris.privamov.ops.OpsModule

object AccioAppMain extends AccioApp

/**
 * Core flags used at the Accio-level. Commands may define additional flags.
 *
 * @param logLevel Logging level.
 */
case class AccioOpts(
  @Flag(name = "logging", help = "Logging level")
  logLevel: String = "warn",
  @Flag(name = "acciorc", help = "Path to the .acciorc configuration file")
  accioRcPath: Option[Path],
  @Flag(name = "config")
  accioRcConfig: Option[String])

/**
 * Entry point of the Accio command line application. Very little is done here, it is the job of [[Command]]s
 * to actually handle the payload.
 */
class AccioApp extends StrictLogging {
  def main(args: Array[String]): Unit = {
    // Change the path of logback's configuration file to match Pants resource naming.
    sys.props("logback.configurationFile") = "fr/cnrs/liris/accio/cli/logback.xml"

    val reporter = new StreamReporter(Console.out, useColors = true)
    val injector = Guice.createInjector(FlagsModule, FrameworkModule, CliModule, OpsModule, LocalRuntimeModule)
    val dispatcher = injector.getInstance(classOf[CommandDispatcher])
    val exitCode = dispatcher.exec(args, reporter)

    logger.info(s"Terminating Accio client: ${exitCode.name}")
    sys.exit(exitCode.code)
  }
}