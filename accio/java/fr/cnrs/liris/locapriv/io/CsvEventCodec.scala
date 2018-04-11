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
import fr.cnrs.liris.locapriv.model.Event
import fr.cnrs.liris.util.geo.LatLng
import fr.cnrs.liris.util.ByteUtils
import org.joda.time.Instant

import scala.reflect.{ClassTag, classTag}

/**
 * Codec for our CSV format handling events.
 *
 * @param charset Charset to use when decoding a line.
 */
final class CsvEventCodec(charset: Charset = Charsets.UTF_8) extends Codec[Event] {
  override def elementClassTag: ClassTag[Event] = classTag[Event]

  override def encode(key: String, elements: Seq[Event]): Array[Byte] = {
    ByteUtils.foldLines(elements.map(encodeEvent))
  }

  override def decode(key: String, bytes: Array[Byte]): Seq[Event] = {
    new String(bytes, charset)
      .split("\n")
      .toSeq
      .flatMap(line => decodeEvent(key, line.trim))
  }

  private def encodeEvent(event: Event) = {
    val latLng = event.point.toLatLng
    Seq(event.user, latLng.lat.degrees, latLng.lng.degrees, event.time.getMillis)
      .mkString(",")
      .getBytes(charset)
  }

  private def decodeEvent(key: String, line: String) = {
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