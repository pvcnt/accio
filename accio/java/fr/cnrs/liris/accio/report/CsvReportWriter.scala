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

package fr.cnrs.liris.accio.report

import java.nio.file.{Files, Path, StandardOpenOption}

import fr.cnrs.liris.accio.validation.thrift._
import fr.cnrs.liris.accio.validation.{Utils, Values}

import scala.collection.JavaConverters._

/**
 * Create CSV reports from results of previous runs.
 *
 * @param separator Separator to use in CSV files.
 * @param split     Whether to split the reports per combination of workflow parameters.
 * @param aggregate Whether to aggregate artifact values across multiple runs into a single value.
 * @param append    Whether to allow appending data to existing files if they already exists
 */
final class CsvReportWriter(separator: String, split: Boolean, aggregate: Boolean, append: Boolean) {
  /**
   * Write the values of artifacts in CSV format inside the given directory.
   *
   * @param artifacts Artifacts to create a report for.
   * @param workDir   Directory where to write CSV reports (doesn't have to exist).
   */
  def write(artifacts: ArtifactList, workDir: Path): Unit = {
    if (split) {
      // When splitting, one sub-directory is created per combination of workflow parameters.
      artifacts.split.foreach { list =>
        val filename = Utils.label(list.params).replace(" ", ",")
        doWrite(list, workDir.resolve(filename))
      }
    } else {
      // When not splitting, we write report directory inside the working directory.
      doWrite(artifacts, workDir)
    }
  }

  private def doWrite(list: ArtifactList, workDir: Path): Unit = {
    Files.createDirectories(workDir)
    list.groups.foreach { group =>
      val header = asHeader(group.kind)
      val artifacts = if (aggregate) Seq(group.aggregated) else group.toSeq
      val rows = artifacts.flatMap(artifact => asString(artifact.value))
      val lines = (Seq(header) ++ rows).map(_.mkString(separator))
      val filename = group.name.replace("/", "-")
      doWrite(lines, workDir, filename)
    }
  }

  private def doWrite(lines: Seq[String], workDir: Path, filename: String): Unit = {
    if (append) {
      val file = workDir.resolve(s"$filename.csv")
      Files.write(file, lines.asJava, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)
    } else {
      val file = getFileName(workDir, filename, ".csv")
      Files.write(file, lines.asJava)
    }
  }

  private def getFileName(dirPath: Path, prefix: String, suffix: String) = {
    var idx = 0
    var filePath = dirPath.resolve(prefix + suffix)
    while (filePath.toFile.exists) {
      idx += 1
      filePath = dirPath.resolve(s"$prefix-$idx$suffix")
    }
    filePath
  }

  private def asHeader(kind: DataType): Seq[String] =
    kind match {
      case DataType.ListType(ListType(values)) => asHeader(values)
      case DataType.MapType(MapType(_, values)) => Seq("key", "key_index") ++ asHeader(values)
      case DataType.Dataset(_) => Seq("value")
      case DataType.Atomic(tpe) => asHeader(tpe)
      case DataType.UnknownUnionField(_) => throw new IllegalArgumentException("Unknown data type")
    }

  private def asHeader(kind: AtomicType): Seq[String] =
    kind match {
      case AtomicType.Distance => Seq("value_in_meters")
      case AtomicType.Duration => Seq("value_in_millis")
      case _ => Seq("value")
    }

  private def asString(value: Value): Seq[Seq[String]] =
    value.dataType match {
      case DataType.ListType(tpe) => Values.decodeList(value, tpe).map(v => Seq(v.toString))
      case DataType.MapType(tpe) =>
        val map = Values.decodeMap(value, tpe)
        val keysIndex = map.keySet.zipWithIndex.toMap
        map.toSeq.map { case (k, v) =>
          val kIdx = keysIndex(k.asInstanceOf[Any])
          Seq(k.toString, kIdx.toString, v.toString)
        }
      case DataType.Dataset(tpe) => Seq(Seq(Values.decodeDataset(value, tpe).uri))
      case DataType.Atomic(AtomicType.Distance) => Seq(Seq(Values.decodeDistance(value).meters.toString))
      case DataType.Atomic(AtomicType.Duration) => Seq(Seq(Values.decodeDuration(value).getMillis.toString))
      case DataType.Atomic(_) => Seq(Seq(Values.decode(value).toString))
      case DataType.UnknownUnionField(_) => throw new IllegalArgumentException("Unknown data type")
    }
}