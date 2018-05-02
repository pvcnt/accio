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

import fr.cnrs.liris.locapriv.domain.Poi
import fr.cnrs.liris.util.geo.LatLng
import org.joda.time.Instant

import scala.reflect._

/**
 * Support for [[http://snap.stanford.edu/ Snap datasets]] encoding POIs from social networks.
 */
final class SnapDecoder extends Decoder[Poi] {
  override def elementClassTag: ClassTag[Poi] = classTag[Poi]

  override def decode(key: String, bytes: Array[Byte]): Seq[Poi] = {
    val line = new String(bytes)
    val parts = line.trim.split("\t")
    if (parts.length < 5) {
      Seq.empty
    } else {
      val username = parts(0)
      val time = Instant.parse(parts(1))
      val lat = parts(2).toDouble
      val lng = parts(3).toDouble
      Seq(Poi(username, LatLng.degrees(lat, lng).toPoint, time))
    }
  }
}