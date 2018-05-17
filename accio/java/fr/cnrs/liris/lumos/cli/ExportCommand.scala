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

package fr.cnrs.liris.lumos.cli

/*final class ExportCommand extends Command with ClientCommand {
  private[this] val outFlag = flag[String](
    "out",
    "Directory where to write the export")
  private[this] val separatorFlag = flag(
    "separator",
    " ",
    "Separator to use in generated files")
  private[this] val artifactsFlag = flag(
    "artifacts",
    "NUMERIC",
    "Comma-separated list of artifacts to take into account. Special values: ALL, NUMERIC (for " +
      "only those of a numeric type), NONE.")
  private[this] val splitFlag = flag(
    "split",
    false,
    "Whether to split the export by workflow parameters")
  private[this] val aggregateFlag = flag(
    "aggregate",
    false,
    "Whether to aggregate artifact values across multiple runs into a single value")
  private[this] val appendFlag = flag(
    "append",
    false,
    "Whether to allow appending data to existing files if they already exists")

  override def name = "export"

  override def help = "Generate text reports from run results."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: CommandEnvironment): Future[ExitCode] = {
    if (residue.isEmpty) {
      env.reporter.handle(Event.error("You must specify at least one run as argument."))
      return Future.value(ExitCode.CommandLineError)
    }
    val workDir = getWorkDir
    env.reporter.handle(Event.info(s"Writing export to ${workDir.toAbsolutePath}"))

    getJobs(residue, env.reporter).map { jobs =>
      env.reporter.handle(Event.info(s"Found ${jobs.size} matching runs"))
      val artifacts = getArtifacts(jobs)
      val writer = new CsvReportWriter(
        separator = separatorFlag(),
        split = splitFlag(),
        aggregate = aggregateFlag(),
        append = appendFlag())
      writer.write(artifacts, workDir)
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

  private def getJobs(residue: Seq[String], reporter: Reporter) = {
    val fs = residue.map { name =>
      client
        .getJob(GetJobRequest(name))
        .flatMap { resp =>
          if (resp.job.status.children.isDefined) {
            client.listJobs(ListJobsRequest(parent = Some(resp.job.name))).map(_.jobs)
          } else {
            Future.value(Seq(resp.job))
          }
        }
    }
    Future.collect(fs).map(jobs => new AggregatedJobs(jobs.flatten))
  }

  private def getArtifacts(runs: AggregatedJobs): ArtifactList =
    artifactsFlag() match {
      case "NONE" | "" => ArtifactList(Seq.empty, Seq.empty, Seq.empty)
      case str => runs.artifacts.filter(StringUtils.explode(str, ","))
    }
}*/