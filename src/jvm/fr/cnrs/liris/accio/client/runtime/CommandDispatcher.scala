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

package fr.cnrs.liris.accio.client.runtime

import java.io.PrintStream

import ch.qos.logback.classic.{Level, Logger}
import com.google.common.io.Flushables
import com.google.inject.{Inject, Injector}
import com.twitter.util.{Duration, Stopwatch}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.client.event.{Event, EventHandler, Reporter}
import fr.cnrs.liris.common.flags.{Flag, FlagsParser, FlagsParsingException, Priority}
import fr.cnrs.liris.common.io.OutErr
import fr.cnrs.liris.common.util.TimeUtils
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

private[runtime] case class CommonCommandOpts(
  @Flag(name = "logging", help = "Logging level")
  logLevel: String = "warn")

/**
 * The command dispatcher is responsible for discovering the command to execute, instantiating it and executing it.
 *
 * @param registry Command registry.
 * @param injector Guice injector.
 */
class CommandDispatcher @Inject()(registry: CommandRegistry, injector: Injector) extends StrictLogging {
  /**
   * Execute the appropriate command given some arguments.
   *
   * @param args Input arguments.
   * @return Exit code.
   */
  def exec(args: Seq[String], outErr: OutErr): ExitCode = {
    val elapsed = Stopwatch.start()
    val (cmdName, positionalArgs) = if (args.isEmpty) ("help", Seq.empty[String]) else (args.head, args.tail)
    registry.get(cmdName) match {
      case None =>
        outErr.printErrLn(s"Command '$cmdName' not found. Type 'accio help'.")
        ExitCode.CommandLineError
      case Some(meta) =>
        val parser = createFlagsParser(meta.defn)
        try {
          parseArgsAndConfigs(parser, positionalArgs)
        } catch {
          case e: FlagsParsingException =>
            outErr.printErrLn(e.getMessage)
            return ExitCode.CommandLineError
        }

        // Configure logging level.
        val commonOpts = parser.as[CommonCommandOpts]
        setupLogging(Level.toLevel(commonOpts.logLevel))

        // Do this before an actual crash so we don't have to worry about
        // allocating memory post-crash.
        //String[] crashData = env.getCrashData();
        //int numericExitCode = ExitCode.BLAZE_INTERNAL_ERROR.getNumericExitCode();

        val savedOut = System.out
        val savedErr = System.err

        val handler = createEventHandler(outErr, parser.as[CommandEventHandlerOpts])
        val reporter = new Reporter(Seq(handler))

        try {
          // While a command is active, direct all errors to the client's event handler (and out/err streams).
          val reporterOutErr = reporter.outErr
          System.setOut(new PrintStream(reporterOutErr.out, true))
          System.setErr(new PrintStream(reporterOutErr.err, true))

          // Print warnings for odd options usage.
          parser.warnings.foreach(warning => reporter.handle(Event.warn(warning)))

          try {
            val command = injector.getInstance(meta.cmdClass)
            val exitCode = command.execute(parser, reporter)
            reporter.handle(Event.info(s"Elapsed: ${TimeUtils.prettyTime(elapsed())}"))
            exitCode
          } catch {
            case NonFatal(e) =>
              // Here are only handled "unexpected" exception. It means that common error cases (e.g., Thrift
              // communication errors, invalid files) should be handled in commands. We treat exceptions caught here
              // as fatal exception (it could be OOM exceptions), this is why they won't be very nicely formatted.
              logger.error("Uncaught exception", e)
              //BugReport.printBug(outErr, e);
              //BugReport.sendBugReport(e, args, crashData);
              ExitCode.InternalError
          }
        } finally {
          Flushables.flushQuietly(outErr.out)
          Flushables.flushQuietly(outErr.err)

          System.setOut(savedOut)
          System.setErr(savedErr)
          releaseEventHandler(handler)
        }
    }
  }

  private def createFlagsParser(cmdDef: Cmd) = {
    val flagClasses = cmdDef.flags ++ Seq(classOf[CommonCommandOpts], classOf[CommandEventHandlerOpts])
    FlagsParser(cmdDef.allowResidue, flagClasses: _*)
  }

  private def parseArgsAndConfigs(parser: FlagsParser, args: Seq[String]) = {
    parser.parse(args, Priority.CommandLine, Some("command line options"))
  }

  private def setupLogging(level: Level) = {
    LoggerFactory.getLogger("fr.cnrs.liris.accio").asInstanceOf[Logger].setLevel(level)
    logger.info(s"Set logging level: $level")
  }

  private def createEventHandler(outErr: OutErr, opts: CommandEventHandlerOpts) = {
    val handler = if (opts.useColor || opts.useCurses) {
      new FancyTerminalEventHandler(outErr, opts)
    } else {
      new CommandEventHandler(outErr, opts)
    }
    if (opts.showProgressRateLimit > Duration.Zero) {
      new RateLimitingEventHandler(handler, opts.showProgressRateLimit)
    } else {
      handler
    }
  }

  /**
   * Unsets the event handler.
   */
  private def releaseEventHandler(handler: EventHandler) = {
    handler match {
      case h: FancyTerminalEventHandler =>
        // Make sure that the terminal state of the old event handler is clear before creating a new one.
        h.resetTerminal()
      case _ => // Do nothing special.
    }
  }
}