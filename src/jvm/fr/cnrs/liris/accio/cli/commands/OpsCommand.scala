package fr.cnrs.liris.accio.cli.commands

import java.io.InputStreamReader

import com.google.common.io.CharStreams
import com.google.inject.Inject
import fr.cnrs.liris.accio.cli.{Command, Reporter}
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.common.flags.FlagsProvider
import fr.cnrs.liris.common.util.TextUtils

@Command(
  name = "ops",
  help = "Provide information about registered operators"
)
class OpsCommand @Inject()(opRegistry: OpRegistry) extends AccioCommand[(HelpFlags)] {
  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      printSummary(out)
      ExitCode.Success
    } else if (flags.residue.size == 1) {
      opRegistry.get(flags.residue.head) match {
        case Some(meta) =>
          printOperator(out, meta, flags.as[HelpFlags])
          ExitCode.Success
        case None =>
          out.writeln(s"<error>Unknown operator '${flags.residue.head}'</error>")
          ExitCode.CommandLineError
      }
    } else {
      out.writeln("<error>You must specify only one operator</error>")
      ExitCode.CommandLineError
    }
  }

  private def printSummary(out: Reporter) = {
    val maxLength = opRegistry.ops.map(_.defn.name.length).max
    opRegistry.ops.groupBy(_.defn.category).foreach { case (category, ops) =>
      out.writeln(s"<info>Operators in $category category:</info>")
      ops.foreach { op =>
        val padding = " " * (maxLength - op.defn.name.length)
        out.writeln(s"  <comment>${op.defn.name}</comment>$padding ${op.defn.help.getOrElse("")}")
      }
      out.writeln()
    }
  }

  private def printOperator(out: Reporter, meta: OpMeta, opts: HelpFlags) = {
    out.write(s"<comment>${meta.defn.name}</comment>")
    if (meta.defn.unstable || meta.defn.ephemeral) {
      out.write(" (")
      out.write(((if (meta.defn.unstable) Seq("unstable") else Seq.empty) ++
          (if (meta.defn.ephemeral) Seq("ephemeral") else Seq.empty)).mkString(", "))
      out.write(")")
    }
    out.writeln()
    out.writeln()
    meta.defn.help.foreach { help =>
      out.writeln(help)
      out.writeln()
    }
    meta.defn.description.foreach { description =>
      val text = if (description.startsWith("resource:")) {
        CharStreams.toString(new InputStreamReader(meta.clazz.getResourceAsStream(description.substring(9))))
      } else {
        description
      }
      out.writeln(TextUtils.paragraphFill(text, 80))
      out.writeln()
    }

    printOperatorInputs(out, meta, opts)
    printOperatorParams(out, meta, opts)
    printOperatorOutputs(out, meta, opts)
  }

  private def printOperatorParams(out: Reporter, meta: OpMeta, opts: HelpFlags) = {
    out.writeln(s"<info>Parameters:</info>")
    meta.defn.params.foreach { paramDef =>
      out.write(s"  - ${paramDef.name} (type: ${paramDef.typ.toString.toLowerCase}")
      if (paramDef.defaultValue.isDefined && paramDef.defaultValue.get != None) {
        out.write(s"; default: ${paramDef.defaultValue.get}")
      }
      if (paramDef.optional) {
        out.write("; optional")
      }
      out.write(")")
      out.writeln()
      if (opts.long && paramDef.help.isDefined) {
        out.writeln(TextUtils.paragraphFill(paramDef.help.get, 80, 4))
      }
    }
  }

  private def printOperatorInputs(out: Reporter, meta: OpMeta, opts: HelpFlags) = {
    if (meta.defn.inputs.nonEmpty) {
      out.writeln("<info>Inputs:</info>")
      meta.defn.inputs.foreach { inputDef =>
        out.write(s"  - ${inputDef.name} (type: dataset)")
        out.writeln()
        if (opts.long && inputDef.help.isDefined) {
          out.writeln(TextUtils.paragraphFill(inputDef.help.get, 80, 4))
        }
      }
    }
  }

  private def printOperatorOutputs(out: Reporter, meta: OpMeta, opts: HelpFlags) = {
    if (meta.defn.outputs.nonEmpty) {
      out.writeln("<info>Outputs:</info>")
      meta.defn.outputs.foreach { outputDef =>
        out.write(s"  - ${outputDef.name} (type: ${outputDef.`type`})")
        out.writeln()
        if (opts.long && outputDef.help.isDefined) {
          out.writeln(TextUtils.paragraphFill(outputDef.help.get, 80, 4))
        }
      }
    }
  }
}
