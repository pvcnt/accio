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

package fr.cnrs.liris.accio.discovery

import fr.cnrs.liris.accio.domain.{Attribute, Operator}
import fr.cnrs.liris.lumos.domain.{DataType, RemoteFile, Value}

object testing {
  val ops = Seq(
    Operator(
      name = "FirstSimple",
      executable = RemoteFile("."),
      inputs = Seq(Attribute("foo", DataType.Int)),
      outputs = Seq(Attribute("data", DataType.Dataset))),
    Operator(
      name = "SecondSimple",
      executable = RemoteFile("."),
      inputs = Seq(
        Attribute("dbl", DataType.Double),
        Attribute("str", DataType.String, defaultValue = Some(Value.String("something"))),
        Attribute("data", DataType.Dataset)),
      outputs = Seq(Attribute("data", DataType.Dataset))),
    Operator(
      name = "ThirdSimple",
      executable = RemoteFile("."),
      inputs = Seq(
        Attribute("data1", DataType.Dataset),
        Attribute("data2", DataType.Dataset)),
      outputs = Seq(Attribute("data", DataType.Dataset))))
}
