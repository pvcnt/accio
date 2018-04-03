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

import com.twitter.util.Future
import fr.cnrs.liris.accio.tools.cli.event.{Event, Reporter}

final class HelpCommand extends Command {
  override def name = "help"

  override def help = "Display built-in Accio help."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: CommandEnvironment): Future[ExitCode] = {
    residue match {
      case Seq() =>
        printHelpSummary(env.registry, env.reporter)
        Future.value(ExitCode.Success)
      case Seq(helpTopic) =>
        // Help topic.
        env.registry.get(helpTopic) match {
          case Some(command) =>
            printCommand(env.reporter, command)
            Future.value(ExitCode.Success)
          case None =>
            // Unknown help topic.
            env.reporter.handle(Event.error(s"Unknown command: $helpTopic"))
            Future.value(ExitCode.CommandLineError)
        }
      case _ =>
        // Too much arguments.
        env.reporter.handle(Event.error("You must specify only one help topic"))
        Future.value(ExitCode.CommandLineError)
    }
  }

  private def printHelpSummary(registry: CommandRegistry, reporter: Reporter): Unit = {
    reporter.outErr.printOutLn("Usage: accio <command> <options>...")
    reporter.outErr.printOutLn()
    reporter.outErr.printOutLn("Available commands:")
    val maxLength = registry.commands.filterNot(_.hidden).map(_.name.length).max
    registry.commands.toSeq.sortBy(_.name).foreach { command =>
      val padding = " " * (maxLength - command.name.length)
      reporter.outErr.printOutLn(s"  ${command.name}$padding ${command.help}")
    }
    reporter.outErr.printOutLn()
    reporter.outErr.printOutLn("Getting more help:")
    reporter.outErr.printOutLn("  accio help <command> Print help and options for <command>.")
  }

  private def printCommand(out: Reporter, command: Command): Unit = {
    out.outErr.printOutLn(s"Usage: accio ${command.name} <options> ${if (command.allowResidue) "<arguments>" else ""}")
    out.outErr.printOutLn()
    if (command.help.nonEmpty) {
      out.outErr.printOutLn(command.help)
      out.outErr.printOutLn()
    }
    val flags = command.flag.getAll(includeGlobal = false)
    if (flags.nonEmpty) {
      out.outErr.printOutLn(s"Available options:")
      flags.foreach(flag => out.outErr.printOutLn(s"  - ${flag.usageString}"))
    }
  }
}