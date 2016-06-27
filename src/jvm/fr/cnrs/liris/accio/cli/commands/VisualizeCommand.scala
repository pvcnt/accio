package fr.cnrs.liris.accio.cli.commands

import java.io.{FileOutputStream, PrintStream}
import java.nio.file.Paths
import java.util.UUID

import com.google.inject.Inject
import fr.cnrs.liris.accio.cli.{Command, HtmlReportCreator, ReportStatistics, Reporter}
import fr.cnrs.liris.accio.core.pipeline.ReportReader
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.HashUtils

case class VisualizeFlags(
    @Flag(name = "html", help = "Generate an HTML report")
    html: Boolean = false,
    @Flag(name = "gnuplot", help = "Generate Gnuplot graphs")
    gnuplot: Boolean = false,
    @Flag(name = "artifacts", help = "Specify a comma-separated list of artifacts to take into account")
    artifacts: String = "ALL")

@Command(name = "visualize", allowResidue = true)
class VisualizeCommand @Inject()(reportReader: ReportReader) extends AccioCommand[VisualizeFlags] {
  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    val reports = flags.residue.flatMap { path =>
      val workDir = Paths.get(path)
      require(workDir.toFile.exists, s"Directory ${workDir.toAbsolutePath} does not exist")
      workDir.toFile.list
          .filter(_.startsWith("run-"))
          .map(_.drop(4).dropRight(5))
          .map(id => reportReader.readRun(workDir, id))
    }
    val opts = flags.as[VisualizeFlags]
    val reportStats = new ReportStatistics(reports)
    val artifacts = if (opts.artifacts == "ALL") {
      reportStats.artifacts.keySet
    } else {
      opts.artifacts.split(",").map(_.trim).toSet
    }
    val reportCreator = new HtmlReportCreator(showArtifacts = artifacts)
    val uid = HashUtils.sha1(UUID.randomUUID().toString).substring(0, 8)
    val outputPath = Paths.get(s"report-$uid.html")
    reportCreator.print(reportStats, new PrintStream(new FileOutputStream(outputPath.toFile)))
    out.writeln(s"Report written to <comment>${outputPath.toAbsolutePath}</comment>")
    ExitCode.Success
  }
}