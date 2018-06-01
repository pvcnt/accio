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

package fr.cnrs.liris.infra.cli.app

import java.util.Locale

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.helpers.NOPAppender
import com.twitter.util.logging.Slf4jBridgeUtility
import fr.cnrs.liris.infra.cli.io.OutErr
import org.slf4j.{Logger, LoggerFactory}

trait Application {
  def name: String

  def productName: String = name.toLowerCase(Locale.ROOT)

  def commands: Set[Command]

  final def builtinCommands: Set[Command] = Set(new HelpCommand)

  /**
   * Return the command with a given name, if it exists.
   *
   * @param name Command name.
   */
  final def get(name: String): Option[Command] = (commands ++ builtinCommands).find(_.name == name)

  final def main(args: Array[String]): Unit = {
    // Prevent from displaying any logs.
    val ctx = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    ctx.reset()
    val rootLogger = ctx.getLogger(Logger.ROOT_LOGGER_NAME)
    val noopAppender = new NOPAppender[ILoggingEvent]
    noopAppender.setContext(ctx)
    noopAppender.start()
    rootLogger.addAppender(noopAppender)

    Slf4jBridgeUtility.attemptSlf4jBridgeHandlerInstallation()

    val dispatcher = new CommandDispatcher(this)
    val exitCode = dispatcher.exec(args, OutErr.System)
    sys.exit(exitCode.code)
  }
}
