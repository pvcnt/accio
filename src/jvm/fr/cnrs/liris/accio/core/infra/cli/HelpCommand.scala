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

package fr.cnrs.liris.accio.core.infra.cli

import com.google.inject.Inject
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.reflect.CaseClass
import fr.cnrs.liris.common.util.StringUtils

@Cmd(
  name = "help",
  help = "Display built-in Accio help.",
  description = "Prints a help page for the given command or help topic, or, if nothing is specified, prints the index of available commands.",
  allowResidue = true)
class HelpCommand @Inject()(cmdRegistry: CmdRegistry) extends Command {
  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    flags.residue match {
      case Seq() =>
        printHelpSummary(out)
        ExitCode.Success
      case Seq(helpTopic) =>
        // Help topic.
        cmdRegistry.get(helpTopic) match {
          case Some(meta) =>
            printCommand(out, meta)
            ExitCode.Success
          case None =>
            // Unknown help topic.
            out.writeln(s"<error>[ERROR]</error> Unknown command: $helpTopic")
            ExitCode.CommandLineError
        }
      case _ =>
        // Too much arguments.
        out.writeln("<error>[ERROR]</error> You must specify only one help topic.")
        ExitCode.CommandLineError
    }
  }

  private def printHelpSummary(out: Reporter) = {
    out.writeln("Usage: accio <command> <options>...")
    out.writeln()
    out.writeln("<info>Available commands:</info>")
    val maxLength = cmdRegistry.commands.filterNot(_.defn.hidden).map(_.defn.name.length).max
    cmdRegistry.commands.toSeq.sortBy(_.defn.name).foreach { command =>
      val padding = " " * (maxLength - command.defn.name.length)
      out.writeln(s"  <comment>${command.defn.name}</comment>$padding ${command.defn.help}")
    }
    out.writeln()
    out.writeln("Getting more help:")
    out.writeln("  <comment>accio help <command></comment> Print help and options for <command>.")
  }

  private def printCommand(out: Reporter, meta: CmdMeta) = {
    out.writeln(s"Usage: accio ${meta.defn.name} <options> ${if (meta.defn.allowResidue) "<arguments>" else ""}")
    out.writeln()
    if (meta.defn.help.nonEmpty) {
      out.writeln(meta.defn.help)
      out.writeln()
    }
    if (meta.defn.description.nonEmpty) {
      out.writeln(meta.defn.description)
      out.writeln()
    }
    val flags = meta.defn.flags.map(CaseClass.apply(_)).flatMap(_.fields)
    if (flags.nonEmpty) {
      out.writeln(s"<info>Available options:</info>")
      flags.foreach { field =>
        val flag = field.annotation[Flag]
        out.write(s"  - ${flag.name} (type: ${field.scalaType.runtimeClass.getSimpleName.toLowerCase}")
        if (field.defaultValue.isDefined && field.defaultValue.get != None) {
          out.write(s"; default: ${field.defaultValue.get}")
        }
        if (field.isOption) {
          out.write("; optional")
        }
        out.write(")")
        out.writeln()
        if (flag.help.nonEmpty) {
          out.writeln(StringUtils.paragraphFill(flag.help, 80, 4))
        }
      }
    }
  }
}