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

package fr.cnrs.liris.accio.dsl.py

import com.twitter.io.Buf
import fr.cnrs.liris.accio.domain.Workflow
import fr.cnrs.liris.accio.dsl.WorkflowParser

final class PyWorkflowParser extends WorkflowParser {
  override def decode(buf: Buf): Workflow = {
    val str = buf match {
      case Buf.Utf8(s) => s
    }
    println(Statements.single_input.parse(str).get.value)
    Workflow()
  }
}

object PyWorkflowParserMain {
  def main(args: Array[String]): Unit = {
    val parser = new PyWorkflowParser
    parser.decode(Buf.Utf8(
      """uri = param("/path/to/geolife")
      |d = DatasetReader(uri).data
      |def Metrics(d1, d2):
      |  SpatialDistortion(d1, d2)
      |  PoisRetrieval(ExtractPois(d1), ExtractPois(d2))
      |Metrics(d, GeoI(d, 0.01).data)
      |Metrics(d, Promesse(d, 200).data)""".stripMargin))
  }
}
