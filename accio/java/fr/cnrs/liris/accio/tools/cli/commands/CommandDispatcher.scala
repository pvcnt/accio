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

package fr.cnrs.liris.accio.tools.cli.commands

import java.io.PrintStream

import com.google.common.io.Flushables
import com.twitter.app.Flags
import com.twitter.util.{Event => _, _}
import fr.cnrs.liris.accio.api.thrift.{ErrorCode, ServerException}
import fr.cnrs.liris.accio.tools.cli.event._
import fr.cnrs.liris.accio.tools.cli.terminal.OutErr
import fr.cnrs.liris.common.util.TimeUtils

import scala.util.control.NonFatal

/**
 * The command dispatcher is responsible for discovering the command to execute, instantiating it and executing it.
 *
 * @param registry Command registry.
 */
final class CommandDispatcher(registry: CommandRegistry) {
  /**
   * Execute the appropriate command given some arguments.
   *
   * @param args Input arguments.
   * @return Exit code.
   */
  def exec(args: Array[String], outErr: OutErr): ExitCode = {
    val elapsed = Stopwatch.start()
    val (cmdName, positionalArgs) = if (args.isEmpty) ("help", Array.empty[String]) else (args.head, args.tail)
    registry.get(cmdName) match {
      case None =>
        outErr.printErrLn(s"Command '$cmdName' not found. Type 'accio help'.")
        ExitCode.CommandLineError
      case Some(command) =>
        command.flag.parseArgs(positionalArgs, allowUndefinedFlags = false) match {
          case Flags.Ok(residue) => exec(command, residue, outErr, elapsed)
          case Flags.Error(reason) =>
            outErr.printErrLn(reason)
            ExitCode.CommandLineError
          case Flags.Help(usage) =>
            outErr.printOutLn(usage)
            ExitCode.Success
        }
    }
  }

  private def exec(command: Command, residue: Seq[String], outErr: OutErr, elapsed: Stopwatch.Elapsed): ExitCode = {
    // Do this before an actual crash so we don't have to worry about
    // allocating memory post-crash.
    //String[] crashData = env.getCrashData();
    //int numericExitCode = ExitCode.BLAZE_INTERNAL_ERROR.getNumericExitCode();

    val savedOut = System.out
    val savedErr = System.err

    val handler = createEventHandler(outErr, command.terminalOptions)
    val reporter = new Reporter(Seq(handler))
    val env = createEnvironment(reporter)

    try {
      // While a command is active, direct all errors to the client's event handler (and out/err streams).
      System.setOut(new PrintStream(reporter.outErr.out, true))
      System.setErr(new PrintStream(reporter.outErr.err, true))

      try {
        val exitCode = tryExec(command, residue, env)
        reporter.handle(Event.info(s"Elapsed: ${TimeUtils.prettyTime(elapsed())}"))
        exitCode
      } catch {
        case NonFatal(e) =>
          // Here are only handled "unexpected" exception. It means that common error cases (e.g., Thrift
          // communication errors, invalid files) should be handled in commands. We treat exceptions caught here
          // as fatal exception (it could be OOM exceptions), this is why they won't be very nicely formatted.
          e.printStackTrace()
          // BugReport.printBug(outErr, e);
          // BugReport.sendBugReport(e, args, crashData);
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

  private def tryExec(command: Command, residue: Seq[String], env: CommandEnvironment): ExitCode = {
    val f = command.execute(residue, env).handle {
      case e: ServerException =>
        e.message.foreach { message =>
          env.reporter.handle(Event.error(message))
        }
        e.details.foreach { details =>
          details.warnings.foreach { violation =>
            env.reporter.handle(Event(EventKind.Warning, s"${violation.message} (at ${violation.field})"))
          }
          details.errors.foreach { violation =>
            env.reporter.handle(Event(EventKind.Error, s"${violation.message} (at ${violation.field})"))
          }
        }
        e.code match {
          case ErrorCode.InvalidArgument => ExitCode.DefinitionError
          case ErrorCode.NotFound => ExitCode.CommandLineError
          case ErrorCode.FailedPrecondition => ExitCode.CommandLineError
          case _ => ExitCode.InternalError
        }
      case NonFatal(e) =>
        env.reporter.handle(Event.warn(s"Server error: ${e.getMessage}"))
        ExitCode.InternalError
    }
    Await.result(f)
  }

  private def createEnvironment(reporter: Reporter): CommandEnvironment = {
    new CommandEnvironment(registry, reporter)
  }

  private def createEventHandler(outErr: OutErr, options: TerminalEventHandlerOptions) = {
    val handler = if (options.useColor || options.useCurses) {
      new FancyTerminalEventHandler(outErr, options)
    } else {
      new TerminalEventHandler(outErr, options)
    }
    if (options.showProgressRateLimit > Duration.Zero) {
      new RateLimitingEventHandler(handler, options.showProgressRateLimit)
    } else {
      handler
    }
  }

  /**
   * Unsets the event handler.
   */
  private def releaseEventHandler(handler: EventHandler): Unit = {
    handler match {
      case h: FancyTerminalEventHandler =>
        // Make sure that the terminal state of the old event handler is clear before creating a new one.
        h.resetTerminal()
      case _ => // Do nothing special.
    }
  }
}