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

import fr.cnrs.liris.dal.core.io.Codec
import fr.cnrs.liris.privamov.core.model.PoiSet

import scala.reflect._

/**
 * Codec for our CSV format handling POIs sets.
 */
final class CsvPoiSetCodec extends Codec[PoiSet] {
  private[this] val poiCodec = new CsvPoiCodec

  override def elementClassTag: ClassTag[PoiSet] = classTag[PoiSet]

  override def encode(key: String, elements: Seq[PoiSet]): Array[Byte] = {
    poiCodec.encode(key, elements.flatMap(_.pois))
  }

  override def decode(key: String, bytes: Array[Byte]): Seq[PoiSet] = {
    val pois = poiCodec.decode(key, bytes)
    if (pois.nonEmpty) Seq(PoiSet(key, pois)) else Seq.empty
  }
}