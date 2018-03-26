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

package fr.cnrs.liris.accio.tools.cli

import com.twitter.inject.app.App
import fr.cnrs.liris.accio.logging.LogbackConfigurator
import fr.cnrs.liris.accio.tools.cli.command.{CommandDispatcher, CommandModule}
import fr.cnrs.liris.accio.tools.cli.config.ConfigModule
import fr.cnrs.liris.common.io.OutErr

object AccioClientMain extends AccioClient

/**
 * Entry point of the Accio command line application.
 */
class AccioClient extends App with LogbackConfigurator {
  override def modules = Seq(CommandModule, ConfigModule)

  override def failfastOnFlagsNotParsed = false

  override def allowUndefinedFlags = true

  override def run(): Unit = {
    val dispatcher = injector.instance[CommandDispatcher]
    val exitCode = dispatcher.exec(args, OutErr.System)
    logger.debug(s"Terminating Accio client: ${exitCode.name}")
    sys.exit(exitCode.code)
  }
}