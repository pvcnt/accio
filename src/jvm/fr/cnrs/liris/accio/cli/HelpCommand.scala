package fr.cnrs.liris.accio.cli

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.framework.OpRegistry
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.reflect.CaseClass
import fr.cnrs.liris.common.util.TextUtils

@Command(
  name = "help",
  help = "Display Accio help",
  allowResidue = true)
class HelpCommand @Inject()(commandRegistry: CommandRegistry, opRegistry: OpRegistry) extends AccioCommand {
  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      printSummary(out)
    } else {
      commandRegistry.get(flags.residue.head) match {
        case Some(meta) => printCommand(out, meta)
        case None => out.writeln(s"<error>Unknown command '${flags.residue.head}'</error>")
      }
    }
    ExitCode.Success
  }

  private def printSummary(out: Reporter) = {
    out.writeln("Usage: accio <command> <options>...")
    out.writeln()
    out.writeln("<info>Available commands:</info>")
    val maxLength = commandRegistry.commands.filterNot(_.defn.hidden).map(_.defn.name.length).max
    commandRegistry.commands.foreach { command =>
      val padding = " " * (maxLength - command.defn.name.length)
      out.writeln(s"  <comment>${command.defn.name}</comment>$padding ${command.defn.help}")
    }
    out.writeln()
    out.writeln("Getting more help:")
    out.writeln("  <comment>accio help <command></comment> Print help and options for <command>")
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
        out.write(s"  - ${flag.name} (type: ${field.scalaType.runtimeClass.getSimpleName.toString.toLowerCase}")
        if (field.defaultValue.isDefined && field.defaultValue.get != None) {
          out.write(s"; default: ${field.defaultValue.get}")
        }
        if (field.isOption) {
          out.write("; optional")
        }
        out.write(")")
        out.writeln()
        if (flag.help.nonEmpty) {
          out.writeln(TextUtils.paragraphFill(flag.help, 80, 4))
        }
      }
    }
  }
}