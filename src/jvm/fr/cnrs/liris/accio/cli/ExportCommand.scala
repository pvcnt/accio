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

import java.nio.file.{Files, Paths}
import java.util.UUID

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.framework.{Artifact, DataType, ReportRepository, Values}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.HashUtils

import scala.collection.JavaConverters._

case class ExportOpts(
  @Flag(name = "sep", help = "Separator to use in generated files")
  separator: String = ",",
  @Flag(name = "artifacts", help = "Specify a comma-separated list of artifacts to take into account, or ALL for " +
    "all of them, or NUMERIC for only those of a numeric type")
  artifacts: String = "NUMERIC",
  @Flag(name = "runs", help = "Specify a comma-separated list of runs/experiments to take into account")
  runs: String = "")

@Cmd(
  name = "export",
  flags = Array(classOf[ExportOpts]),
  help = "Generate CSV reports.",
  allowResidue = true)
class ExportCommand @Inject()(repository: ReportRepository) extends AccioCommand {

  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    val reports = flags.residue.flatMap { path =>
      val workDir = Paths.get(path)
      require(workDir.toFile.exists, s"Directory ${workDir.toAbsolutePath} does not exist")
      workDir.toFile.list
        .filter(_.startsWith("run-"))
        .map(_.drop(4).dropRight(5))
        .flatMap(id => repository.readRun(workDir, id))
    }
    val opts = flags.as[ExportOpts]
    val reportStats = new ReportStatistics(reports)
    val artifacts = filterRuns(filterArtifacts(reportStats.artifacts, opts.artifacts), opts.runs)

    val uid = HashUtils.sha1(UUID.randomUUID().toString).substring(0, 8)
    val outDir = Paths.get(s"export-$uid")
    Files.createDirectories(outDir)
    artifacts.foreach { case (artifactName, arts) =>
      val lines = if (arts.nonEmpty) {
        val header = asHeader(arts.values.head.kind)
        val rows = arts.flatMap { case (runId, artifact) =>
          asString(artifact.kind, artifact.value).map(items => Seq(runId) ++ items)
        }
        (Seq(header) ++ rows).map(_.mkString(opts.separator))
      } else Seq.empty[String]
      Files.write(outDir.resolve(s"${artifactName.replace("/", "__")}.csv"), lines.asJava)
    }

    out.writeln(s"Export written to <comment>${outDir.toAbsolutePath}</comment>")
    ExitCode.Success
  }

  private def filterArtifacts(artifacts: Map[String, Map[String, Artifact]], filter: String): Map[String, Map[String, Artifact]] = {
    if (filter == "ALL") {
      artifacts
    } else if (filter == "NUMERIC") {
      artifacts
        .map { case (name, arts) => name -> arts.filter { case (k, v) => v.kind.isNumeric } }
        .filter { case (_, arts) => arts.nonEmpty }
    } else {
      val validNames = filter.split(",").map(_.trim).toSet
      artifacts.filter { case (name, _) => validNames.contains(name) }
    }
  }

  private def filterRuns(artifacts: Map[String, Map[String, Artifact]], filter: String): Map[String, Map[String, Artifact]] = {
    if (filter.isEmpty) {
      artifacts
    } else {
      val validIds = filter.split(",").map(_.trim).toSet
      artifacts
        .map { case (name, arts) => name -> arts.filter { case (runId, _) => validIds.exists(id => runId.startsWith(id)) } }
        .filter { case (_, arts) => arts.nonEmpty }
    }
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