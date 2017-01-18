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

package fr.cnrs.liris.privamov.core.io

import java.nio.file.{Files, Paths}

import com.github.nscala_time.time.Imports._
import com.google.common.base.{Charsets, MoreObjects}
import fr.cnrs.liris.common.geo.{Distance, LatLng}
import fr.cnrs.liris.common.util.FileUtils
import fr.cnrs.liris.privamov.core.model.{Event, Poi, PoiSet, Trace}
import org.joda.time.Instant

import scala.reflect._

/**
 * Data source reading data from our CSV format. There is one CSV file per source key.
 *
 * @param uri     Path to the directory from where to read.
 * @param decoder Decoder to read elements from each CSV file.
 * @tparam T Type of elements being read.
 */
class CsvSource[T: ClassTag](uri: String, decoder: Decoder[T]) extends DataSource[T] {
  private[this] val path = Paths.get(FileUtils.expand(uri))
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

  override def read(id: String): Option[T] = {
    val bytes = Files.readAllBytes(path.resolve(s"$id.csv"))
    decoder.decode(id, bytes)
  }

  override def toString: String =
    MoreObjects.toStringHelper(this)
      .addValue(classTag[T].runtimeClass.getName)
      .add("uri", uri)
      .toString

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
 * Decoder for our CSV format handling traces.
 */
class CsvTraceDecoder extends Decoder[Trace] {
  private[this] val decoder = new TextLineDecoder(new CsvEventDecoder)

  override def decode(key: String, bytes: Array[Byte]): Option[Trace] = {
    val events = decoder.decode(key, bytes).getOrElse(Seq.empty).sortBy(_.time)
    Some(Trace(key, events))
  }

  override def elementClassTag: ClassTag[Trace] = classTag[Trace]
}

/**
 * Decoder for our CSV format handling events.
 */
class CsvEventDecoder extends Decoder[Event] {
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

  override def elementClassTag: ClassTag[Event] = classTag[Event]
}

/**
 * Decoder for our CSV format handling POIs sets.
 */
class CsvPoiSetDecoder extends Decoder[PoiSet] {
  private[this] val decoder = new TextLineDecoder(new CsvPoiDecoder)

  override def decode(key: String, bytes: Array[Byte]): Option[PoiSet] = {
    val pois = decoder.decode(key, bytes).getOrElse(Seq.empty)
    Some(PoiSet(key, pois))
  }

  override def elementClassTag: ClassTag[PoiSet] = classTag[PoiSet]
}

/**
 * Decoder for our CSV format handling POIs.
 */
class CsvPoiDecoder extends Decoder[Poi] {
  override def decode(key: String, bytes: Array[Byte]): Option[Poi] = {
    val line = new String(bytes, Charsets.UTF_8).trim
    val parts = line.split(",")
    if (parts.length < 7) {
      None
    } else {
      val user = parts(0)
      val point = LatLng.degrees(parts(1).toDouble, parts(2).toDouble).toPoint
      val size = parts(3).toInt
      val firstSeen = new Instant(parts(4).toLong)
      val lastSeen = new Instant(parts(5).toLong)
      val diameter = Distance.meters(parts(6).toDouble)
      Some(Poi(user, point, size, firstSeen, lastSeen, diameter))
    }
  }

  override def elementClassTag: ClassTag[Poi] = classTag[Poi]
}