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

package fr.cnrs.liris.accio.client

import java.nio.file.Path

import com.google.inject.Guice
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.client.service.ParserFinatraJacksonModule
import fr.cnrs.liris.accio.core.infra.cli.{CmdDispatcher, StreamReporter}
import fr.cnrs.liris.common.flags._

object AccioClientMain extends AccioClient

/**
 * Core flags used at the Accio-level. Commands may define additional flags.
 *
 * @param logLevel Logging level.
 */
case class AccioFlags(
  @Flag(name = "logging", help = "Logging level")
  logLevel: String = "warn",
  @Flag(name = "color", help = "Enable or disabled colored output")
  color: Boolean = false,
  @Flag(name = "rc", help = "Path to the .acciorc configuration file")
  accioRcPath: Option[Path],
  @Flag(name = "config")
  accioRcConfig: Option[String])

/**
 * Entry point of the Accio command line application. Very little is done here, it is the job of [[fr.cnrs.liris.accio.core.infra.cli.Command]]s
 * to actually handle the payload.
 */
class AccioClient extends StrictLogging {
  def main(args: Array[String]): Unit = {
    // Change the path of logback's configuration file to match Pants resource naming.
    sys.props("logback.configurationFile") = "fr/cnrs/liris/accio/client/logback.xml"

    //val parser = FlagsParser[AccioFlags](allowResidue = true)
    //parser.parse(args)

    val injector = Guice.createInjector(/*FlagsModule(parser), */ClientModule, ParserFinatraJacksonModule)
    val dispatcher = injector.getInstance(classOf[CmdDispatcher])
    val exitCode = dispatcher.exec(args)

    logger.info(s"Terminating Accio client: ${exitCode.name}")
    sys.exit(exitCode.code)
  }
}