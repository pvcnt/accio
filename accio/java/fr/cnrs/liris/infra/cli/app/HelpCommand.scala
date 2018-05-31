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

import com.twitter.util.Future

final class HelpCommand extends Command {
  override def name = "help"

  override def help = "Display built-in help."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: Environment): Future[ExitCode] = {
    residue match {
      case Seq() =>
        printHelpSummary(env)
        Future.value(ExitCode.Success)
      case Seq(helpTopic) =>
        // Help topic.
        env.application.get(helpTopic) match {
          case Some(command) =>
            printCommand(command, env)
            Future.value(ExitCode.Success)
          case None =>
            // Unknown help topic.
            env.reporter.error(s"Unknown command: $helpTopic")
            Future.value(ExitCode.CommandLineError)
        }
      case _ =>
        // Too much arguments.
        env.reporter.error("You must specify only one help topic")
        Future.value(ExitCode.CommandLineError)
    }
  }

  private def printHelpSummary(env: Environment): Unit = {
    env.reporter.outErr.printOutLn(s"Usage: ${env.application.productName} <command> <options>...")
    env.reporter.outErr.printOutLn()
    env.reporter.outErr.printOutLn("Available commands:")
    val commands = env.application.commands ++ env.application.builtinCommands
    val maxLength = commands.filterNot(_.hidden).map(_.name.length).max
    commands.toSeq.sortBy(_.name).foreach { command =>
      val padding = " " * (maxLength - command.name.length)
      env.reporter.outErr.printOutLn(s"  ${command.name}$padding ${command.help}")
    }
    env.reporter.outErr.printOutLn()
    env.reporter.outErr.printOutLn("Getting more help:")
    env.reporter.outErr.printOutLn(s"  ${env.application.productName} help <command> Print help and options for <command>.")
  }

  private def printCommand(command: Command, env: Environment): Unit = {
    env.reporter.outErr.printOutLn(s"Usage: ${env.application.productName} ${command.name} <options> ${if (command.allowResidue) "<arguments>" else ""}")
    env.reporter.outErr.printOutLn()
    if (command.help.nonEmpty) {
      env.reporter.outErr.printOutLn(command.help)
      env.reporter.outErr.printOutLn()
    }
    val flags = command.flag.getAll(includeGlobal = false)
    if (flags.nonEmpty) {
      env.reporter.outErr.printOutLn(s"Available options:")
      flags.foreach(flag => env.reporter.outErr.printOutLn(s"  - ${flag.usageString}"))
    }
  }
}