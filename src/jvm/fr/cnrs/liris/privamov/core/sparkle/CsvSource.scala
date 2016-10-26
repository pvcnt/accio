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

package fr.cnrs.liris.privamov.core.sparkle

import java.io.File
import java.nio.file.{Files, Paths}

import com.github.nscala_time.time.Imports._
import com.google.common.base.Charsets
import fr.cnrs.liris.common.geo.LatLng
import fr.cnrs.liris.common.util.FileUtils
import fr.cnrs.liris.privamov.core.model.{Event, Trace}
import org.joda.time.Instant

/**
 * Mobility traces source reading data from our custom CSV format, with one file per trace.
 *
 * @param uri URI to directory where to read.
 */
case class CsvSource(uri: String) extends DataSource[Trace] {
  private[this] val path = Paths.get(FileUtils.replaceHome(uri))
  private[this] val decoder = new TextLineDecoder(new CsvDecoder)
  require(path.toFile.isDirectory, s"$uri is not a directory")
  require(path.toFile.canRead, s"$uri is unreadable")

  override def keys: Seq[String] = {
    path.toFile
      .listFiles
      .filter(_.getName.endsWith(".csv"))
      .map(_.getName.stripSuffix(".csv"))
      .toSeq
      .sortWith(sort)

  }

  override def read(id: String): Iterable[Trace] = Seq(read(id, path.resolve(s"$id.csv").toFile))

  private def read(id: String, file: File): Trace = {
    val events = decoder.decode(id, Files.readAllBytes(file.toPath)).getOrElse(Seq.empty).sortBy(_.time)
    Trace(id, events)
  }

  private def sort(key1: String, key2: String): Boolean = {
    val parts1 = key1.split("-").tail.map(_.toInt)
    val parts2 = key2.split("-").tail.map(_.toInt)
    if (parts1.isEmpty && parts2.isEmpty) {
      key1 < key2
    } else if (parts1.isEmpty) {
      true
    } else if (parts2.isEmpty) {
      false
    } else {
      sort(parts1, parts2)
    }
  }

  private def sort(parts1: Seq[Int], parts2: Seq[Int]): Boolean = {
    parts1.zip(parts2).find { case (a, b) => a != b }.exists { case (a, b) => a < b }
  }
}

/**
 * Mobility traces sink writing data to our custom CSV format, with one file per trace.
 *
 * @param uri URI to directory where to write.
 */
case class CsvSink(uri: String) extends DataSink[Trace] {
  private[this] val path = Paths.get(FileUtils.replaceHome(uri))
  if (!path.toFile.exists) {
    Files.createDirectories(path)
  } else if (path.toFile.isDirectory && path.toFile.listFiles.nonEmpty) {
    throw new IllegalArgumentException(s"Non-empty directory: ${path.toAbsolutePath}")
  }
  private[this] val encoder = new CsvEncoder
  private[this] val NL = "\n".getBytes(Charsets.UTF_8)

  override def write(elements: Iterator[Trace]): Unit = {
    elements.foreach { trace =>
      val encodedEvents = trace.events.map(encoder.encode)
      val bytes = if (encodedEvents.isEmpty) {
        Array.empty[Byte]
      } else if (encodedEvents.size == 1) {
        encodedEvents.head
      } else {
        encodedEvents.tail.fold(encodedEvents.head)(_ ++ NL ++ _)
      }
      Files.write(path.resolve(s"${trace.id}.csv"), bytes)
    }
  }
}

/**
 * Decoder for our custom CSV format handling events.
 */
class CsvDecoder extends Decoder[Event] {
  override def decode(key: String, bytes: Array[Byte]): Option[Event] = {
    val line = new String(bytes, Charsets.UTF_8).trim
    val parts = line.split(",")
    if (parts.length < 3 || parts.length > 4) {
      None
    } else {
      val (user, lat, lng, time) = if (parts.length == 4) {
        (parts(0), parts(1).toDouble, parts(2).toDouble, parts(3).toLong)
      } else {
        (key, parts(0).toDouble, parts(1).toDouble, parts(2).toLong)
      }
      val point = LatLng.degrees(lat, lng).toPoint
      Some(Event(user, point, new Instant(time)))
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