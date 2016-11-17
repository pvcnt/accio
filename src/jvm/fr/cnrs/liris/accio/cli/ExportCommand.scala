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

import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.util.UUID

import com.google.inject.Inject
import com.twitter.util.Stopwatch
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.accio.core.reporting.{AggregatedRuns, ArtifactList}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.{FileUtils, HashUtils, StringUtils, TimeUtils}

import scala.collection.JavaConverters._

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
    val elapsed = Stopwatch.start()
    val runs = flags.residue.flatMap { path =>
      val workDir = Paths.get(path)
      require(workDir.toFile.exists, s"Directory ${workDir.toAbsolutePath} does not exist")
      workDir.toFile.list
        .filter(_.startsWith("run-"))
        .map(_.drop(4).dropRight(5))
        .flatMap(id => repository.readRun(workDir, id))
    }
    val opts = flags.as[ExportOptions]
    val aggRuns = new AggregatedRuns(runs).filter(StringUtils.explode(opts.runs, ","))
    val aggArtifacts = aggRuns.artifacts.filter(StringUtils.explode(opts.artifacts))

    val workDir = getWorkDir(opts)

    if (opts.split) {
      aggArtifacts.split.foreach { list =>
        val label = list.params.map { case (k, v) => s"$k=$v" }.mkString(",")
        write(list, workDir.resolve(label), opts)
      }
    } else {
      write(aggArtifacts, workDir, opts)
    }

    out.writeln(s"Export available in <comment>${workDir.toAbsolutePath}</comment>")
    out.writeln(s"Done in ${TimeUtils.prettyTime(elapsed())}.")
    ExitCode.Success
  }

  private def getWorkDir(opts: ExportOptions): Path =
    opts.workDir match {
      case Some(dir) => Paths.get(FileUtils.replaceHome(dir))
      case None =>
        val uid = HashUtils.sha1(UUID.randomUUID().toString).substring(0, 8)
        Paths.get(s"accio-export-$uid")
    }

  private def write(list: ArtifactList, workDir: Path, opts: ExportOptions): Unit = {
    Files.createDirectories(workDir)
    list.groups.foreach { group =>
      val artifacts = if (opts.aggregate) Seq(group.aggregated) else group.toSeq
      val header = asHeader(group.kind)
      val rows = artifacts.flatMap(artifact => asString(artifact.kind, artifact.value))
      val lines = (Seq(header) ++ rows).map(_.mkString(opts.separator))
      if (opts.append) {
        val file = Paths.get(s"${group.name.replace("/", "-")}.csv")
        Files.write(file, lines.asJava, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
      } else {
        val file = getFileName(workDir, s"${group.name.replace("/", "-")}", ".csv")
        Files.write(file, lines.asJava)
      }
    }
  }

  private def getFileName(dirPath: Path, prefix: String, suffix: String) = {
    var idx = 0
    var filePath = dirPath.resolve(prefix + suffix)
    while (!dirPath.toFile.exists) {
      idx += 1
      filePath = dirPath.resolve(s"$prefix-$idx$suffix")
    }
    filePath
  }

  private def asHeader(kind: DataType): Seq[String] = kind match {
    case DataType.List(of) => asHeader(of)
    case DataType.Set(of) => asHeader(of)
    case DataType.Map(ofKeys, ofValues) => Seq("key_index", "key") ++ asHeader(ofValues)
    case DataType.Distance => Seq("value_in_meters")
    case DataType.Duration => Seq("value_in_millis")
    case _ => Seq("value")
  }

  private def asString(kind: DataType, value: Any): Seq[Seq[String]] = kind match {
    case DataType.List(of) => Values.asList(value, of).flatMap(asString(of, _))
    case DataType.Set(of) => Values.asSet(value, of).toSeq.flatMap(asString(of, _))
    case DataType.Map(ofKeys, ofValues) =>
      val map = Values.asMap(value, ofKeys, ofValues)
      val keysIndex = map.keys.zipWithIndex.toMap
      map.toSeq.flatMap { case (k, v) =>
        val kIdx = keysIndex(k.asInstanceOf[Any]).toString
        asString(ofKeys, k).flatMap { kStrs =>
          kStrs.flatMap { kStr =>
            asString(ofValues, v).map(vStrs => Seq(kIdx, kStr) ++ vStrs)
          }
        }
      }
    case DataType.Distance => Seq(Seq(Values.asDistance(value).meters.toString))
    case DataType.Duration => Seq(Seq(Values.asDuration(value).getMillis.toString))
    case _ => Seq(Seq(Values.as(value, kind).toString))
  }
}