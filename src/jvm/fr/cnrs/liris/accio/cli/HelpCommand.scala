package fr.cnrs.liris.accio.cli

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.framework.OpRegistry
import fr.cnrs.liris.common.flags.FlagsProvider

@Command(name = "help", help = "Display Accio help")
class HelpCommand @Inject()(commandRegistry: CommandRegistry, opRegistry: OpRegistry) extends AccioCommand[Unit] {
  override def run(flags: FlagsProvider, out: Reporter): ExitCode = {
    commandRegistry.commands.foreach { command =>
      out.writeln(s"  <comment>${command.defn.name}</comment>: ${command.defn.help.getOrElse("")}")
    }
    ExitCode.Success
  }
}