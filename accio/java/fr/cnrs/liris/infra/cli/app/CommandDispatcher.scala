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

import java.io.PrintStream

import com.google.common.io.Flushables
import com.twitter.app.Flags
import com.twitter.util._
import fr.cnrs.liris.infra.cli.io.OutErr
import fr.cnrs.liris.infra.thriftserver.{ErrorCode, ServerError}

import scala.util.control.NonFatal

/**
 * The command dispatcher is responsible for discovering the command to execute, instantiating it
 * and executing it.
 *
 * @param application Application instance.
 */
final class CommandDispatcher(application: Application) {
  /**
   * Execute the appropriate command given some arguments.
   *
   * @param args Input arguments.
   * @return Exit code.
   */
  def exec(args: Array[String], outErr: OutErr): ExitCode = {
    val elapsed = Stopwatch.start()
    val (cmdName, positionalArgs) = if (args.isEmpty) ("help", Array.empty[String]) else (args.head, args.tail)
    application.get(cmdName) match {
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

    val reporter: Reporter = new AnsiTerminalReporter(outErr, command.terminalOptions)
    val env = new Environment(application, reporter)

    try {
      // While a command is active, direct all errors to the client's event handler (and out/err streams).
      System.setOut(new PrintStream(reporter.outErr.out, true))
      System.setErr(new PrintStream(reporter.outErr.err, true))

      try {
        val exitCode = tryExec(command, residue, env)
        reporter.info(s"Elapsed: ${humanize(elapsed())}")
        exitCode
      } catch {
        case NonFatal(e) =>
          // Here are only handled "unexpected" exception. It means that common error cases
          // (e.g., Thrift communication errors, invalid files) should be handled in commands. We
          // treat exceptions caught here as fatal, this is why they won't be very nicely formatted.
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
      releaseReporter(reporter)
    }
  }

  private def humanize(duration: Duration): String = {
    val ms = duration.inMillis.toDouble
    if (ms < 10.0) {
      f"$ms%.2f ms"
    } else if (ms < 100.0) {
      f"$ms%.1f ms"
    } else if (ms < 1000.0) {
      f"$ms%.0f ms"
    } else {
      f"${ms / 1000}%.3f s"
    }
  }

  private def tryExec(command: Command, residue: Seq[String], env: Environment): ExitCode = {
    val f = command.execute(residue, env).handle {
      case e: ServerError if e.code == ErrorCode.InvalidArgument =>
        env.reporter.error(e.message.getOrElse("The request has some errors."))
        e.errors.toSeq.flatten.foreach { error =>
          env.reporter.error(s"${error.message} at ${error.field}")
        }
        ExitCode.DefinitionError
      case e: ServerError if e.code == ErrorCode.NotFound =>
        env.reporter.error(e.message.getOrElse("The resource was not found."))
        ExitCode.CommandLineError
      case e: ServerError if e.code == ErrorCode.Unauthenticated =>
        env.reporter.error(e.message.getOrElse("The provided credentials are invalid."))
        ExitCode.CommandLineError
      case NonFatal(e) =>
        env.reporter.error(e.getMessage)
        ExitCode.InternalError
    }
    Await.result(f)
  }

  /**
   * Unsets the event handler.
   */
  private def releaseReporter(reporter: Reporter): Unit = {
    reporter match {
      case r: AnsiTerminalReporter =>
        // Make sure that the terminal state of the old event handler is clear before creating a
        // new one.
        r.resetTerminal()
      case _ => // Do nothing special.
    }
  }
}