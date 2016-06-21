package fr.cnrs.liris.accio.cli.commands

import com.google.inject.Inject
import fr.cnrs.liris.accio.cli.{Command, CommandDef, CommandRegistry, Reporter}
import fr.cnrs.liris.accio.core.framework.OpRegistry
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}

case class HelpFlags(
    @Flag(name = "long")
    long: Boolean = false)

@Command(
  name = "help",
  help = "Display Accio help",
  allowResidue = true)
class HelpCommand @Inject()(commandRegistry: CommandRegistry, opRegistry: OpRegistry) extends AccioCommand[HelpFlags] {
  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      printSummary(out)
    } else {
      commandRegistry.get(flags.residue.head) match {
        case Some(meta) => printCommand(out, meta.defn)
        case None => out.writeln(s"<error>Unknown command '${flags.residue.head}'</error>")
      }
    }
    ExitCode.Success
  }

  private def printSummary(out: Reporter) = {
    out.writeln("Usage: accio <command> <options>...")
    out.writeln()
    out.writeln("<info>Available commands:</info>")
    val maxLength = commandRegistry.commands.map(_.defn.name.length).max
    commandRegistry.commands.foreach { command =>
      val padding = " " * (maxLength - command.defn.name.length)
      out.writeln(s"  <comment>${command.defn.name}</comment>$padding ${command.defn.help.getOrElse("")}")
    }
    out.writeln()
    out.writeln("Getting more help:")
    out.writeln("  <comment>accio help <command></comment> Prints help and options for <command>")
  }

  private def printCommand(out: Reporter, defn: CommandDef) = {
    out.writeln(s"Usage: accio ${defn.name} <options> ${if (defn.allowResidue) "<arguments>" else ""}")
    out.writeln()
    defn.help.foreach { help =>
      out.writeln(help)
      out.writeln()
    }
    defn.description.foreach { description =>
      out.writeln(description)
      out.writeln()
    }
  }
}