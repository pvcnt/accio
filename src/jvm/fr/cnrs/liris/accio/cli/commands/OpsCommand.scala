package fr.cnrs.liris.accio.cli.commands

import com.google.inject.Inject
import fr.cnrs.liris.accio.cli.{Command, Reporter}
import fr.cnrs.liris.accio.core.framework.{Analyzer, _}
import fr.cnrs.liris.common.flags.FlagsProvider
import fr.cnrs.liris.common.util.TextUtils

@Command(name = "ops", help = "Provide information about registered operators")
class OpsCommand @Inject()(opRegistry: OpRegistry) extends AccioCommand[(HelpFlags)] {
  override def run(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      printSummary(out)
    } else {
      opRegistry.get(flags.residue.head) match {
        case Some(meta) => printOperator(out, meta, flags.as[HelpFlags])
        case None => out.writeln(s"<error>Unknown operator '${flags.residue.head}'</error>")
      }
    }
    ExitCode.Success
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
      out.writeln(description)
      out.writeln()
    }

    printOperatorParams(out, meta, opts)
    printOperatorIO(out, meta, opts)
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

  private def printOperatorIO(out: Reporter, meta: OpMeta, opts: HelpFlags) = {
    if (classOf[Transformer].isAssignableFrom(meta.clazz)) {
      out.writeln("<info>Inputs:</info>")
      out.write("  - data (type: dataset)")
      out.writeln()
      if (opts.long) {
        out.writeln("    Input dataset of traces")
      }
      out.writeln()
      out.writeln("<info>Outputs:</info>")
      out.write("  - data (type: dataset)")
      if (opts.long) {
        out.writeln("    Output dataset of traces that has been transformed")
      }
      out.writeln()
    } else if (classOf[Evaluator].isAssignableFrom(meta.clazz)) {
      out.writeln("<info>Inputs:</info>")
      out.write("  - train (type: dataset)")
      out.writeln()
      if (opts.long) {
        out.writeln("    Training dataset of traces (contains normally reference traces, the ground truth)")
      }
      out.write("  - test (type: dataset)")
      out.writeln()
      if (opts.long) {
        out.writeln("    Testing dataset of traces (contains normally protected traces)")
      }
      out.writeln()
    } else if (classOf[Analyzer].isAssignableFrom(meta.clazz)) {
      out.writeln("<info>Inputs:</info>")
      out.write("  - data (type: dataset)")
      out.writeln()
      if (opts.long) {
        out.writeln("    Input dataset of traces")
      }
      out.writeln()
    } else if (classOf[Source].isAssignableFrom(meta.clazz)) {
      out.writeln("<info>Outputs:</info>")
      out.write("  - data (type: dataset)")
      out.writeln()
      if (opts.long) {
        out.writeln("    Source dataset of traces")
      }
      out.writeln()
    }
  }
}
