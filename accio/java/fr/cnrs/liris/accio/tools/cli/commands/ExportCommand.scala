/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

package fr.cnrs.liris.accio.tools.cli.commands

import java.nio.file.{Path, Paths}
import java.util.UUID

import com.twitter.util.Await
import fr.cnrs.liris.accio.agent.GetRunRequest
import fr.cnrs.liris.accio.api.thrift.RunId
import fr.cnrs.liris.accio.reporting._
import fr.cnrs.liris.accio.tools.cli.event.{Event, Reporter}
import fr.cnrs.liris.common.util.{FileUtils, HashUtils, StringUtils}

final class ExportCommand extends Command with ClientCommand {
  private[this] val outFlag = flag[String]("out", "Directory where to write the export")
  private[this] val separatorFlag = flag("separator", " ", "Separator to use in generated files")
  private[this] val artifactsFlag = flag("artifacts", "NUMERIC", "Comma-separated list of artifacts to take into account. Special values: ALL, NUMERIC (for only those of a numeric type), NONE.")
  private[this] val metricsFlag = flag("metrics", "NONE", "Comma-separated list of metrics to export. Special values: ALL, NONE.")
  private[this] val splitFlag = flag("split", false, "Whether to split the export by workflow parameters")
  private[this] val aggregateFlag = flag("aggregate", false, "Whether to aggregate artifact values across multiple runs into a single value")
  private[this] val appendFlag = flag("append", false, "Whether to allow appending data to existing files if they already exists")

  override def name = "export"

  override def help = "Generate text reports from run results."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: CommandEnvironment): ExitCode = {
    if (residue.isEmpty) {
      env.reporter.outErr.printOutLn("<error>[ERROR]</error> You must specify at least one run as argument.")
      ExitCode.CommandLineError
    } else {
      val workDir = getWorkDir
      env.reporter.handle(Event.info(s"Writing export to ${workDir.toAbsolutePath}"))

      val runs = getRuns(residue, env.reporter)
      env.reporter.handle(Event.info(s"Found ${runs.size} matching runs"))

      val artifacts = getArtifacts(runs)
      val metrics = getMetrics(runs)
      val reportCreator = new CsvReportCreator
      val reportCreatorOpts = CsvReportOptions(
        separator = separatorFlag(),
        split = splitFlag(),
        aggregate = aggregateFlag(),
        append = appendFlag())
      reportCreator.write(artifacts, workDir, reportCreatorOpts)
      reportCreator.write(metrics, workDir, reportCreatorOpts)
      ExitCode.Success
    }
  }

  private def getWorkDir: Path =
    outFlag.get match {
      case Some(dir) => FileUtils.expandPath(dir)
      case None =>
        val uid = HashUtils.sha1(UUID.randomUUID().toString).substring(0, 8)
        Paths.get(s"accio-export-$uid")
    }

  private def getRuns(residue: Seq[String], reporter: Reporter): AggregatedRuns = {
    val runs = residue.flatMap { id =>
      val maybeRun = Await.result(client.getRun(GetRunRequest(RunId(id)))).result
      maybeRun match {
        case None =>
          reporter.handle(Event.warn(s"Unknown run: $id"))
          Seq.empty
        case Some(run) =>
          if (run.children.nonEmpty) {
            run.children.flatMap(childId => Await.result(client.getRun(GetRunRequest(childId))).result)
          } else {
            Seq(run)
          }
      }
    }
    new AggregatedRuns(runs)
  }

  private def getArtifacts(runs: AggregatedRuns): ArtifactList =
    artifactsFlag() match {
      case "NONE" | "" => ArtifactList(Seq.empty, Map.empty, Seq.empty)
      case str => runs.artifacts.filter(StringUtils.explode(str, ","))
    }

  private def getMetrics(runs: AggregatedRuns): MetricList =
    metricsFlag() match {
      case "NONE" | "" => MetricList(Seq.empty, Map.empty, Seq.empty)
      case str => runs.metrics.filter(StringUtils.explode(str, ","))
    }
}