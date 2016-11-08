/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.cli

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.framework.{OpMeta, OpRegistry}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.reflect.CaseClass
import fr.cnrs.liris.common.util.StringUtils

@Cmd(
  name = "help",
  help = "Display built-in Accio help.",
  description = "Prints a help page for the given command or topic, or, if nothing is specified, prints the index of available commands.",
  allowResidue = true)
class HelpCommand @Inject()(commandRegistry: CmdRegistry, opRegistry: OpRegistry) extends Command {

  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    flags.residue match {
      case Seq() =>
        // No argument specified, display help summary.
        printHelpSummary(out)
        ExitCode.Success
      case Seq("list-ops") =>
        // Help topic: display list of registered operators.
        printOpSummary(out)
        ExitCode.Success
      case Seq(helpTopic) =>
        // Help topic.
        commandRegistry.get(helpTopic) match {
          case Some(meta) =>
            // Help topic: display details about a command.
            printCommand(out, meta)
            ExitCode.Success
          case None =>
            opRegistry.get(helpTopic) match {
              // Help topic: display details about an operato.
              case Some(meta) =>
                printOp(out, meta)
                ExitCode.Success
              case None =>
                // Unknown help topic.
                out.writeln(s"<error>No command nor operator named '$helpTopic'</error>")
                ExitCode.CommandLineError
            }
          case _ =>
            // Too much arguments.
            out.writeln("<error>You must specify only one help topic</error>")
            ExitCode.CommandLineError
        }
    }
  }

  private def printHelpSummary(out: Reporter) = {
    out.writeln("Usage: accio <command> <options>...")
    out.writeln()
    out.writeln("<info>Available commands:</info>")
    val maxLength = commandRegistry.commands.filterNot(_.defn.hidden).map(_.defn.name.length).max
    commandRegistry.commands.toSeq.sortBy(_.defn.name).foreach { command =>
      val padding = " " * (maxLength - command.defn.name.length)
      out.writeln(s"  <comment>${command.defn.name}</comment>$padding ${command.defn.help}")
    }
    out.writeln()
    out.writeln("Getting more help:")
    out.writeln("  <comment>accio help <command></comment> Print help and options for <command>.")
    out.writeln("  <comment>accio help <operator></comment> Print help and arguments for <operator>.")
    out.writeln("  <comment>accio help list-ops</comment> Print the list of registered operators.")
  }

  private def printCommand(out: Reporter, meta: CommandMeta) = {
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

  private def printOpSummary(out: Reporter) = {
    val maxLength = opRegistry.ops.map(_.defn.name.length).max
    opRegistry.ops.toSeq.sortBy(_.defn.name).groupBy(_.defn.category).foreach { case (category, ops) =>
      out.writeln(s"<info>Operators in $category category:</info>")
      ops.foreach { op =>
        val padding = " " * (maxLength - op.defn.name.length)
        out.writeln(s"  <comment>${op.defn.name}</comment>$padding ${op.defn.help.getOrElse("")}")
      }
      out.writeln()
    }
  }

  private def printOp(out: Reporter, meta: OpMeta) = {
    out.writeln(s"<comment>${meta.defn.name}</comment> (${meta.defn.category})")
    out.writeln()
    meta.defn.deprecation.foreach { deprecation =>
      out.writeln(s"<error>Deprecated: $deprecation</error>")
      out.writeln()
    }
    meta.defn.help.foreach { help =>
      out.writeln(help)
      out.writeln()
    }
    meta.defn.description.foreach { description =>
      out.writeln(StringUtils.paragraphFill(description, 80))
      out.writeln()
    }
    printOpInputs(out, meta)
    printOpOutputs(out, meta)
  }

  private def printOpInputs(out: Reporter, meta: OpMeta) = {
    out.writeln(s"<info>Available inputs:</info>")
    meta.defn.inputs.foreach { argDef =>
      out.write(s"  - ${argDef.name} (${argDef.kind.typeDescription}")
      if (argDef.defaultValue.isDefined && argDef.defaultValue.get != None) {
        out.write(s"; default: ${argDef.defaultValue.get}")
      }
      if (argDef.isOptional) {
        out.write("; optional")
      }
      out.write(")")
      out.writeln()
      argDef.help.foreach(help => out.writeln(StringUtils.paragraphFill(help, 80, 4)))
    }
  }

  private def printOpOutputs(out: Reporter, meta: OpMeta) = {
    if (meta.defn.outputs.nonEmpty) {
      out.writeln("<info>Available outputs:</info>")
      meta.defn.outputs.foreach { outputDef =>
        out.write(s"  - ${outputDef.name} (${outputDef.kind.typeDescription})")
        out.writeln()
        outputDef.help.foreach(help => out.writeln(StringUtils.paragraphFill(help, 80, 4)))
      }
    }
  }
}