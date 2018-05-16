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

package fr.cnrs.liris.locapriv.testing

import java.io.FileInputStream

import fr.cnrs.liris.locapriv.domain.Event
import fr.cnrs.liris.sparkle.Encoder
import fr.cnrs.liris.sparkle.format.csv.CsvDataFormat

trait WithCabspotting {
  lazy val abboipTrace = cabspottingTrace("abboip")

  def cabspottingTrace(key: String): Seq[Event] = {
    val encoder = Encoder[Event]
    val is = new FileInputStream(s"accio/javatests/fr/cnrs/liris/locapriv/testing/$key.csv")
    try {
      val reader = CsvDataFormat.readerFor(encoder.structType)
      reader.read(is).map(encoder.deserialize).toList
    } finally {
      is.close()
    }
  }
}
