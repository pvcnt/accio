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

package fr.cnrs.liris.locapriv.io

import java.nio.charset.Charset

import com.google.common.base.Charsets
import fr.cnrs.liris.locapriv.model.Poi
import fr.cnrs.liris.util.geo.{Distance, LatLng}
import fr.cnrs.liris.util.ByteUtils
import org.joda.time.Instant

import scala.reflect._

/**
 * Codec for our CSV format handling POIs.
 *
 * @param charset Charset to use when decoding a line.
 */
final class CsvPoiCodec(charset: Charset = Charsets.UTF_8) extends Codec[Poi] {
  override def elementClassTag: ClassTag[Poi] = classTag[Poi]

  override def encode(key: String, elements: Seq[Poi]): Array[Byte] = {
    ByteUtils.foldLines(elements.map(encodePoi))
  }

  override def decode(key: String, bytes: Array[Byte]): Seq[Poi] = {
    new String(bytes, charset)
      .split("\n")
      .toSeq
      .flatMap(line => decodePoi(key, line.trim))
  }

  private def encodePoi(poi: Poi) = {
    val latLng = poi.centroid.toLatLng
    Seq(poi.user, latLng.lat.degrees, latLng.lng.degrees, poi.size, poi.firstSeen.getMillis, poi.lastSeen.getMillis, poi.diameter.meters)
      .mkString(",")
      .getBytes(charset)
  }

  private def decodePoi(key: String, line: String) = {
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
}