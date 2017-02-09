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

import com.google.common.base.Charsets
import fr.cnrs.liris.dal.core.io.Codec
import fr.cnrs.liris.common.geo.LatLng
import fr.cnrs.liris.privamov.core.model.Event
import org.joda.time.Instant

import scala.reflect.{ClassTag, classTag}

/**
 * Codec for our CSV format handling events.
 */
class CsvEventCodec extends Codec[Event] {
  override def encode(obj: Event): Array[Byte] = {
    val latLng = obj.point.toLatLng
    s"${obj.user},${latLng.lat.degrees},${latLng.lng.degrees},${obj.time.getMillis}".getBytes(Charsets.UTF_8)
  }

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