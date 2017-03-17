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

package fr.cnrs.liris.accio.accioctl

import com.google.inject.Guice
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.accioctl.command.inject.BuiltinCommandsModule
import fr.cnrs.liris.accio.accioctl.config.inject.ConfigModule
import fr.cnrs.liris.accio.runtime.cli.CommandDispatcher
import fr.cnrs.liris.accio.runtime.logging.LogbackLogging
import fr.cnrs.liris.common.io.OutErr

object AccioClientMain extends AccioClient

/**
 * Entry point of the Accio command line application.
 */
class AccioClient extends LogbackLogging with StrictLogging {
  def main(args: Array[String]): Unit = {
    val injector = Guice.createInjector(BuiltinCommandsModule, ConfigModule)
    val dispatcher = injector.getInstance(classOf[CommandDispatcher])
    val exitCode = dispatcher.exec(args, OutErr.System)
    logger.debug(s"Terminating Accio client: ${exitCode.name}")
    sys.exit(exitCode.code)
  }
}