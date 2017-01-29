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
import fr.cnrs.liris.accio.core.api.io.Codec
import fr.cnrs.liris.common.geo.{Distance, LatLng}
import fr.cnrs.liris.privamov.core.model.Poi
import org.joda.time.Instant

import scala.reflect._

/**
 * Codec for our CSV format handling POIs.
 */
class CsvPoiCodec extends Codec[Poi] {
  override def encode(obj: Poi): Array[Byte] = {
    val latLng = obj.centroid.toLatLng
    val fields = Seq(obj.user, latLng.lat.degrees, latLng.lng.degrees, obj.size, obj.firstSeen.getMillis, obj.lastSeen.getMillis, obj.diameter.meters)
    fields.mkString(",").getBytes(Charsets.UTF_8)
  }

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