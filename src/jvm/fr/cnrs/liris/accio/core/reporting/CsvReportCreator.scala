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

package fr.cnrs.liris.accio.core.reporting

import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import fr.cnrs.liris.accio.core.framework.{DataType, Values}
import scala.collection.JavaConverters._

case class CsvReportOptions(separator: String, split: Boolean, aggregate: Boolean, append: Boolean)

class CsvReportCreator {
  def write(artifacts: ArtifactList, workDir: Path, opts: CsvReportOptions): Unit = {
    if (opts.split) {
      artifacts.split.foreach { list =>
        val label = list.params.map { case (k, v) => s"$k=$v" }.mkString(",")
        doWrite(list, workDir.resolve(label), opts)
      }
    } else {
      doWrite(artifacts, workDir, opts)
    }
  }

  private def doWrite(list: ArtifactList, workDir: Path, opts: CsvReportOptions): Unit = {
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
