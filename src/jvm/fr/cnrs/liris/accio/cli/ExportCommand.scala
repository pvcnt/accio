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

import java.nio.file.{Path, Paths}
import java.util.UUID

import com.google.inject.Inject
import com.twitter.util.Stopwatch
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.accio.core.reporting.{AggregatedRuns, ArtifactList, CsvReportCreator, CsvReportOptions}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.{FileUtils, HashUtils, StringUtils, TimeUtils}

case class ExportOptions(
  @Flag(name = "workdir", help = "Working directory where to write the export")
  workDir: Option[String],
  @Flag(name = "separator", help = "Separator to use in generated files")
  separator: String = " ",
  @Flag(name = "artifacts", help = "Comma-separated list of artifacts to take into account, or ALL for " +
    "all of them, or NUMERIC for only those of a numeric type")
  artifacts: String = "NUMERIC",
  @Flag(name = "runs", help = "Comma-separated list of runs to take into account")
  runs: Option[String],
  @Flag(name = "split", help = "Whether to split the export by workflow parameters")
  split: Boolean = false,
  @Flag(name = "aggregate", help = "Whether to aggregate artifact values across multiple runs")
  aggregate: Boolean = false,
  @Flag(name = "append", help = "Whether to aggregate exported data to existing file if it already exists")
  append: Boolean = false)

@Cmd(
  name = "export",
  flags = Array(classOf[ExportOptions]),
  help = "Generate text reports from run artifacts.",
  description = "This command is intended to create summarized and readable CSV reports from the output of previous runs.",
  allowResidue = true)
class ExportCommand @Inject()(repository: ReportRepository) extends Command {

  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      out.writeln("<error>Specify one or multiple directories as argument.</error>")
      ExitCode.CommandLineError
    } else {
      val opts = flags.as[ExportOptions]
      val elapsed = Stopwatch.start()

      val workDir = getWorkDir(opts)
      out.writeln(s"Writing export to <comment>${workDir.toAbsolutePath}</comment>")

      val artifacts = getArtifacts(flags.residue, opts)
      val reportCreator = new CsvReportCreator
      val reportCreatorOpts = CsvReportOptions(separator = opts.separator, split = opts.split, aggregate = opts.aggregate, append = opts.append)
      reportCreator.write(artifacts, workDir, reportCreatorOpts)

      out.writeln(s"Done in ${TimeUtils.prettyTime(elapsed())}.")
      ExitCode.Success
    }
  }

  private def getWorkDir(opts: ExportOptions): Path = opts.workDir match {
    case Some(dir) => FileUtils.expandPath(dir)
    case None =>
      val uid = HashUtils.sha1(UUID.randomUUID().toString).substring(0, 8)
      Paths.get(s"accio-export-$uid")
  }

  private def getArtifacts(residue: Seq[String], opts: ExportOptions): ArtifactList = {
    val runs = residue.flatMap { uri =>
      val path = FileUtils.expandPath(uri)
      repository.listRuns(path).flatMap(repository.readRun(path, _))
    }
    val aggRuns = new AggregatedRuns(runs).filter(StringUtils.explode(opts.runs, ","))
    aggRuns.artifacts.filter(StringUtils.explode(opts.artifacts))
  }
}