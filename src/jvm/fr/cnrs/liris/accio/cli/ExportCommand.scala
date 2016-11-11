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

import breeze.stats._
import com.google.inject.Inject
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.geo.Distance
import fr.cnrs.liris.common.util.{HashUtils, Seqs, StringUtils}
import org.joda.time.Duration

import scala.collection.JavaConverters._

case class ExportOptions(
  @Flag(name = "separator", help = "Separator to use in generated files")
  separator: String = " ",
  @Flag(name = "artifacts", help = "Comma-separated list of artifacts to take into account, or ALL for " +
    "all of them, or NUMERIC for only those of a numeric type")
  artifacts: String = "NUMERIC",
  @Flag(name = "runs", help = "Comma-separated list of runs/experiments to take into account")
  runs: Option[String],
  @Flag(name = "aggregate", help = "Whether to aggregate multiple runs of the same graph")
  aggregate: Boolean = true)

@Cmd(
  name = "export",
  flags = Array(classOf[ExportOptions]),
  help = "Generate text reports from run artifacts.",
  allowResidue = true)
class ExportCommand @Inject()(repository: ReportRepository) extends Command {

  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    val startedAt = System.currentTimeMillis()
    val runs = flags.residue.flatMap { path =>
      val workDir = Paths.get(path)
      require(workDir.toFile.exists, s"Directory ${workDir.toAbsolutePath} does not exist")
      workDir.toFile.list
        .filter(_.startsWith("run-"))
        .map(_.drop(4).dropRight(5))
        .flatMap(id => repository.readRun(workDir, id))
    }
    val opts = flags.as[ExportOptions]
    val reportStats = new ReportStatistics(runs)
    val artifacts = maybeAggregate(filterEmpty(filterRuns(filterArtifacts(reportStats.artifacts, opts.artifacts), opts.runs)), runs, opts.aggregate)

    val uid = HashUtils.sha1(UUID.randomUUID().toString).substring(0, 8)
    val outDir = Paths.get(s"export-$uid")
    Files.createDirectories(outDir)
    artifacts.foreach { case (artifactName, arts) =>
      val lines = if (arts.nonEmpty) {
        val header = asHeader(arts.head.kind)
        val rows = arts.flatMap(artifact => asString(artifact.kind, artifact.value))
        (Seq(header) ++ rows).map(_.mkString(opts.separator))
      } else Seq.empty[String]
      Files.write(outDir.resolve(s"${artifactName.replace("/", "-")}.csv"), lines.asJava)
    }

    val duration = System.currentTimeMillis() - startedAt
    out.writeln(s"Done in ${duration / 1000}s. Export in <comment>${outDir.toAbsolutePath}</comment>")
    ExitCode.Success
  }

  private def filterArtifacts(artifacts: Map[String, Map[String, Artifact]], filter: String): Map[String, Map[String, Artifact]] = {
    if (filter == "ALL") {
      artifacts
    } else if (filter == "NUMERIC") {
      artifacts.map { case (name, arts) => name -> arts.filter { case (k, v) => v.kind.isNumeric } }
    } else {
      val validNames = filter.split(",").map(_.trim).toSet
      artifacts.filter { case (name, _) => validNames.contains(name) }
    }
  }

  private def filterRuns(artifacts: Map[String, Map[String, Artifact]], filter: Option[String]): Map[String, Map[String, Artifact]] = {
    if (filter.isEmpty) {
      artifacts
    } else {
      val validIds = StringUtils.explode(filter, ",")
      artifacts.map { case (name, arts) => name -> arts.filter { case (runId, _) => validIds.exists(id => runId.startsWith(id)) } }
    }
  }

  private def filterEmpty(artifacts: Map[String, Map[String, Artifact]]): Map[String, Map[String, Artifact]] =
    artifacts.filter { case (_, arts) => arts.nonEmpty }

  private def maybeAggregate(artifacts: Map[String, Map[String, Artifact]], runs: Seq[Run], agg: Boolean): Map[String, Seq[Artifact]] = {
    /*if (agg) {
      val runsIndex = runs.map(run => run.id -> run).toMap
      artifacts.map { case (name, arts) =>
        // We group artifacts coming from the execution of the same graph.
        val groupedArtifacts = arts.toSeq.groupBy { case (runId, art) => runsIndex(runId).graph }
        // We then aggregate artifacts coming from the execution of the same graph. For now we only support mean aggregation.
        val aggregatedArtifacts = groupedArtifacts.map { case (_, similarArts) => aggregate(similarArts.map(_._2)) }.toSeq
        name -> aggregatedArtifacts
      }
    } else {*/
      artifacts.map { case (name, arts) => name -> arts.values.toSeq }
    //}
  }

  private def aggregate(artifacts: Seq[Artifact]): Artifact = {
    val name = artifacts.head.name
    val kind = artifacts.head.kind
    Artifact(name, kind, aggregate(kind, artifacts.map(_.value)))
  }

  private def aggregate(kind: DataType, values: Seq[Any]): Any = kind match {
    case DataType.Byte => mean(values.map(Values.asDouble))
    case DataType.Short => mean(values.map(Values.asDouble))
    case DataType.Integer => mean(values.map(Values.asDouble))
    case DataType.Double => mean(values.map(Values.asDouble))
    case DataType.Long => mean(values.map(Values.asDouble))
    case DataType.Distance => Distance.meters(mean(values.map(Values.asDistance(_).meters)))
    case DataType.Duration => Duration.millis(mean(values.map(Values.asDuration(_).getMillis.toDouble)).round)
    case DataType.List(of) => aggregate(of, values.flatMap(Values.asList(_, of)))
    case DataType.Set(of) => aggregate(of, values.flatMap(Values.asSet(_, of)))
    case DataType.Map(ofKeys, ofValues) =>
      val valuesByKey = Seqs.index(values.flatMap(Values.asMap(_, ofKeys, ofValues)))
      valuesByKey.map { case (k, vs) => k -> aggregate(ofValues, vs) }
    case _ => throw new IllegalArgumentException(s"Cannot aggregate $kind values")
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