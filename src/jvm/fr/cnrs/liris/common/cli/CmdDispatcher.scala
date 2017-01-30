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

package fr.cnrs.liris.common.cli

import ch.qos.logback.classic.{Level, Logger}
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.common.flags.{FlagsParser, Priority}
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

/**
 * The command dispatcher is responsible for discovering the command to execute, instantiating it and executing it.
 *
 * @param registry Command registry.
 * @param factory  Command factory.
 * @param rcParser Accio RC files parser.
 */
class CmdDispatcher @Inject()(registry: CmdRegistry, factory: CmdFactory, rcParser: AccioRcParser) extends StrictLogging {
  /**
   * Execute the appropriate command given some arguments.
   *
   * @param args Input arguments.
   * @return Exit code.
   */
  def exec(args: Seq[String]): ExitCode = {
    val (cmdName, positionalArgs) =
      if (args.isEmpty || !registry.contains(args.head)) {
        ("help", Seq.empty[String])
      } else {
        (args.head, args.tail)
      }
    val meta = registry(cmdName)

    val parser = FlagsParser(meta.defn.allowResidue, meta.defn.flags ++ Seq(classOf[CliFlags]): _*)
    parser.parseAndExitUponError(positionalArgs)
    val opts = parser.as[CliFlags]

    val accioRcArgs = rcParser.parse(opts.rcPath, opts.rcConfig, cmdName)
    parser.parseAndExitUponError(accioRcArgs, Priority.RcFile)

    val out = new StreamReporter(Console.out, useColors = opts.color)

    if (!registry.contains(cmdName)) {
      out.writeln(s"<error>[ERROR]</error> Unknown command '$cmdName'")
    }

    // Configure logging level for Accio-related code. Other logging configuration is done in an ordinary
    // logback.xml loaded at the very beginning of the main.
    val logLevel = Level.toLevel(opts.logLevel)
    LoggerFactory.getLogger("fr.cnrs.liris.accio").asInstanceOf[Logger].setLevel(logLevel)
    logger.info(s"Set logging level: $logLevel")

    try {
      val command = factory.create(meta)
      command.execute(parser, out)
    } catch {
      case e: IllegalArgumentException =>
        out.writeln(s"<error>[ERROR]</error> ${e.getMessage.stripPrefix("requirement failed: ")}")
        ExitCode.RuntimeError
      case NonFatal(e) =>
        logger.error("Uncaught exception", e)
        ExitCode.InternalError
    }
  }
}