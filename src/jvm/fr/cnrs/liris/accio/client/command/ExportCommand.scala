/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.client.command

import java.nio.file.{Path, Paths}
import java.util.UUID

import com.google.inject.Inject
import com.twitter.util.{Await, Stopwatch}
import fr.cnrs.liris.accio.client.service.AgentClientFactory
import fr.cnrs.liris.accio.core.domain.RunId
import fr.cnrs.liris.accio.core.infra.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.accio.core.service.handler.GetRunRequest
import fr.cnrs.liris.accio.core.service.{AggregatedRuns, ArtifactList, CsvReportCreator, CsvReportOpts}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.{FileUtils, HashUtils, StringUtils, TimeUtils}

case class ExportCommandFlags(
  @Flag(name = "out", help = "Directory where to write the export")
  out: Option[String],
  @Flag(name = "separator", help = "Separator to use in generated files")
  separator: String = " ",
  @Flag(name = "artifacts", help = "Comma-separated list of artifacts to take into account, or NUMERIC for only those of a numeric type")
  artifacts: String = "NUMERIC",
  @Flag(name = "split", help = "Whether to split the export by workflow parameters")
  split: Boolean = false,
  @Flag(name = "aggregate", help = "Whether to aggregate artifact values across multiple runs into a single value")
  aggregate: Boolean = false,
  @Flag(name = "append", help = "Whether to allow appending data to existing files if they already exists")
  append: Boolean = false)

@Cmd(
  name = "export",
  flags = Array(classOf[ExportCommandFlags], classOf[AccioAgentFlags]),
  help = "Generate text reports from run results.",
  description = "This command is intended to create summarized and readable CSV reports from run results.",
  allowResidue = true)
class ExportCommand @Inject()(clientFactory: AgentClientFactory) extends Command {

  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      out.writeln("<error>[ERROR]</error> You must specify at least one run as argument.")
      ExitCode.CommandLineError
    } else {
      val opts = flags.as[ExportCommandFlags]
      val elapsed = Stopwatch.start()

      val workDir = getWorkDir(opts)
      out.writeln(s"<info>[OK]</info> Writing export to <comment>${workDir.toAbsolutePath}</comment>")

      val artifacts = getArtifacts(flags.residue, opts, flags.as[AccioAgentFlags].addr)
      val reportCreator = new CsvReportCreator
      val reportCreatorOpts = CsvReportOpts(separator = opts.separator, split = opts.split, aggregate = opts.aggregate, append = opts.append)
      reportCreator.write(artifacts, workDir, reportCreatorOpts)

      out.writeln(s"<info>[OK]</info> Done in ${TimeUtils.prettyTime(elapsed())}.")
      ExitCode.Success
    }
  }

  private def getWorkDir(opts: ExportCommandFlags): Path = opts.out match {
    case Some(dir) => FileUtils.expandPath(dir)
    case None =>
      val uid = HashUtils.sha1(UUID.randomUUID().toString).substring(0, 8)
      Paths.get(s"accio-export-$uid")
  }

  private def getArtifacts(residue: Seq[String], opts: ExportCommandFlags, addr: String): ArtifactList = {
    val client = clientFactory.create(addr)
    val runs = residue.flatMap { id =>
      Await.result(client.getRun(GetRunRequest(RunId(id)))).result
    }
    val aggRuns = new AggregatedRuns(runs)
    aggRuns.artifacts.filter(StringUtils.explode(opts.artifacts))
  }
}