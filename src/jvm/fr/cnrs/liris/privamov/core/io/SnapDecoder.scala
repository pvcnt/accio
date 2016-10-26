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

import fr.cnrs.liris.common.geo.LatLng
import fr.cnrs.liris.privamov.core.model.Poi
import org.joda.time.Instant

class SnapDecoder extends Decoder[Poi] {
  override def decode(key: String, bytes: Array[Byte]): Option[Poi] = {
    val line = new String(bytes)
    val parts = line.trim.split("\t")
    if (parts.length < 5) {
      None
    } else {
      val username = parts(0)
      val time = Instant.parse(parts(1))
      val lat = parts(2).toDouble
      val lng = parts(3).toDouble
      Some(Poi(username, LatLng.degrees(lat, lng).toPoint, time))
    }
  }
}