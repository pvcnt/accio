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

package fr.cnrs.liris.accio.api

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[Graph]].
 */
class GraphSpec extends UnitSpec {
  behavior of "Graph"

  it should "create a graph" in {
    val struct = thrift.Graph(Seq(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42)))),
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple1",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42)))),
      thrift.Node(
        op = "ThirdSimple",
        name = "ThirdSimple",
        inputs = Map(
          "data1" -> thrift.Input.Reference(thrift.Reference("FirstSimple", "data")),
          "data2" -> thrift.Input.Reference(thrift.Reference("FirstSimple1", "data")))),
      thrift.Node(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> thrift.Input.Value(Values.encodeDouble(3.14)),
          "data" -> thrift.Input.Reference(thrift.Reference("FirstSimple", "data"))))
    ))
    val graph = Graph.fromThrift(struct)
    graph("FirstSimple").outputs should contain theSameElementsAs Map("data" -> Set(
      thrift.Reference("ThirdSimple", "data1"),
      thrift.Reference("SecondSimple", "data")))
    graph("FirstSimple1").outputs should contain theSameElementsAs Map("data" -> Set(
      thrift.Reference("ThirdSimple", "data2")))
  }
}