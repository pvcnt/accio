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

package fr.cnrs.liris.privamov.core.io

import java.nio.file.{Files, Paths}

import com.github.nscala_time.time.Imports._
import com.google.common.base.Charsets
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.common.util.FileUtils
import fr.cnrs.liris.privamov.core.model.{Event, Trace}

/**
 * Mobility traces sink writing data to our custom CSV format, with one file per trace.
 *
 * @param uri                     Path to the directory where to write.
 * @param failOnNonEmptyDirectory Whether to fail is specified directory exists and is not empty.
 */
case class CsvSink(uri: String, failOnNonEmptyDirectory: Boolean = true) extends DataSink[Trace] with LazyLogging {
  private[this] val path = Paths.get(FileUtils.replaceHome(uri))
  if (!path.toFile.exists) {
    Files.createDirectories(path)
  } else if (path.toFile.isDirectory && path.toFile.listFiles.nonEmpty && failOnNonEmptyDirectory) {
    throw new IllegalArgumentException(s"Non-empty directory: ${path.toAbsolutePath}")
  } else if (path.toFile.isFile) {
    throw new IllegalArgumentException(s"${path.toAbsolutePath} already exists and is a file")
  }
  private[this] val encoder = new CsvEncoder
  private[this] val NL = "\n".getBytes(Charsets.UTF_8)

  override def write(elements: TraversableOnce[Trace]): Unit = {
    elements.foreach { trace =>
      val encodedEvents = trace.events.map(encoder.encode)
      val bytes = foldLines(encodedEvents)
      Files.write(path.resolve(s"${trace.id}.csv"), bytes)
    }
  }

  private def foldLines(lines: Seq[Array[Byte]]) = {
    if (lines.isEmpty) {
      // No line, return directly an empty array.
      Array.empty[Byte]
    } else if (lines.size == 1) {
      // A single line, skip costly processing and return it directly.
      lines.head
    } else {
      // A simpler way to do this is: lines.foldLeft(Array.empty[Byte])(_ ++ NL ++ NL).
      // However, I got terribly bad performances doing this; it seems concatenating thousands of arrays is not
      // recommended, so I ended up doing it by end, allocating and filling a single byte array.
      val bytes = Array.ofDim[Byte](lines.map(_.length).sum + NL.length * (lines.size - 1))
      var offset = 0
      lines.zipWithIndex.foreach { case (line, idx) =>
        System.arraycopy(line, 0, bytes, offset, line.length)
        offset += line.length
        if (idx < lines.size - 1) {
          // It is not the last line, we add a line break.
          System.arraycopy(NL, 0, bytes, offset, NL.length)
          offset += NL.length
        }
      }
      bytes
    }
  }
}

/**
 * Encoder for our custom CSV format handling events.
 */
class CsvEncoder extends Encoder[Event] {
  override def encode(obj: Event): Array[Byte] = {
    val latLng = obj.point.toLatLng
    s"${obj.user},${latLng.lat.degrees},${latLng.lng.degrees},${obj.time.millis}".getBytes(Charsets.UTF_8)
  }
}