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

package fr.cnrs.liris.accio.tools.cli.command

import com.google.inject.Inject
import fr.cnrs.liris.accio.runtime.event.{Event, Reporter}
import fr.cnrs.liris.accio.runtime.cli._
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.reflect.CaseClass
import fr.cnrs.liris.common.util.StringUtils

@Cmd(
  name = "help",
  help = "Display built-in Accio help.",
  description = "Prints a help page for the given command or help topic, or, if nothing is specified, prints the index of available commands.",
  allowResidue = true)
class HelpCommand @Inject()(registry: CommandRegistry) extends Command {

  override def execute(flags: FlagsProvider, reporter: Reporter): ExitCode = {
    flags.residue match {
      case Seq() =>
        printHelpSummary(reporter)
        ExitCode.Success
      case Seq(helpTopic) =>
        // Help topic.
        registry.get(helpTopic) match {
          case Some(meta) =>
            printCommand(reporter, meta)
            ExitCode.Success
          case None =>
            // Unknown help topic.
            reporter.handle(Event.error(s"Unknown command: $helpTopic"))
            ExitCode.CommandLineError
        }
      case _ =>
        // Too much arguments.
        reporter.handle(Event.error("You must specify only one help topic"))
        ExitCode.CommandLineError
    }
  }

  private def printHelpSummary(reporter: Reporter) = {
    reporter.outErr.printOutLn("Usage: accio <command> <options>...")
    reporter.outErr.printOutLn()
    reporter.outErr.printOutLn("Available commands:")
    val maxLength = registry.commands.filterNot(_.defn.hidden).map(_.defn.name.length).max
    registry.commands.toSeq.sortBy(_.defn.name).foreach { command =>
      val padding = " " * (maxLength - command.defn.name.length)
      reporter.outErr.printOutLn(s"  ${command.defn.name}$padding ${command.defn.help}")
    }
    reporter.outErr.printOutLn()
    reporter.outErr.printOutLn("Getting more help:")
    reporter.outErr.printOutLn("  accio help <command> Print help and options for <command>.")
  }

  private def printCommand(out: Reporter, meta: CmdMeta) = {
    out.outErr.printOutLn(s"Usage: accio ${meta.defn.name} <options> ${if (meta.defn.allowResidue) "<arguments>" else ""}")
    out.outErr.printOutLn()
    if (meta.defn.help.nonEmpty) {
      out.outErr.printOutLn(meta.defn.help)
      out.outErr.printOutLn()
    }
    if (meta.defn.description.nonEmpty) {
      out.outErr.printOutLn(meta.defn.description)
      out.outErr.printOutLn()
    }
    val flags = meta.defn.flags.map(CaseClass.apply(_)).flatMap(_.fields)
    if (flags.nonEmpty) {
      out.outErr.printOutLn(s"Available options:")
      flags.foreach { field =>
        val flag = field.annotation[Flag]
        out.outErr.printOut(s"  - ${flag.name} (type: ${field.scalaType.runtimeClass.getSimpleName.toLowerCase}")
        if (field.defaultValue.isDefined && field.defaultValue.get != None) {
          out.outErr.printOut(s"; default: ${field.defaultValue.get}")
        }
        if (field.isOption) {
          out.outErr.printOut("; optional")
        }
        out.outErr.printOut(")")
        out.outErr.printOutLn()
        if (flag.help.nonEmpty) {
          out.outErr.printOutLn(StringUtils.paragraphFill(flag.help, 80, 4))
        }
      }
    }
  }
}