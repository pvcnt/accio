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

import java.io.{FileOutputStream, PrintStream}
import java.nio.file.Paths
import java.util.UUID

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.framework.ReportRepository
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.HashUtils

case class VisualizeFlags(
  @Flag(name = "html", help = "Generate an HTML report")
  html: Boolean = false,
  @Flag(name = "artifacts", help = "Specify a comma-separated list of artifacts to take into account")
  artifacts: String = "ALL")

@Cmd(
  name = "visualize",
  flags = Array(classOf[VisualizeFlags]),
  help = "Generate interactive graphs.",
  allowResidue = true)
class VisualizeCommand @Inject()(repository: ReportRepository) extends Command {

  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    val reports = flags.residue.flatMap { path =>
      val workDir = Paths.get(path)
      require(workDir.toFile.exists, s"Directory ${workDir.toAbsolutePath} does not exist")
      workDir.toFile.list
        .filter(_.startsWith("run-"))
        .map(_.drop(4).dropRight(5))
        .flatMap(id => repository.readRun(workDir, id))
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