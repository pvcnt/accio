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

import fr.cnrs.liris.accio.api.thrift.NamedChannel
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[Graph]].
 */
class GraphSpec extends UnitSpec {
  behavior of "Graph"

  it should "create a graph" in {
    val struct = Seq(
      thrift.Step(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Seq(NamedChannel("foo", thrift.Channel.Value(Values.encodeInteger(42))))),
      thrift.Step(
        op = "FirstSimple",
        name = "FirstSimple1",
        inputs = Seq(NamedChannel("foo", thrift.Channel.Value(Values.encodeInteger(42))))),
      thrift.Step(
        op = "ThirdSimple",
        name = "ThirdSimple",
        inputs = Seq(
          NamedChannel("data1", thrift.Channel.Reference(thrift.Reference("FirstSimple", "data"))),
          NamedChannel("data2", thrift.Channel.Reference(thrift.Reference("FirstSimple1", "data"))))),
      thrift.Step(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Seq(
          NamedChannel("dbl", thrift.Channel.Value(Values.encodeDouble(3.14))),
          NamedChannel("data", thrift.Channel.Reference(thrift.Reference("FirstSimple", "data"))))))

    val graph = Graph.fromThrift(struct)
    graph("FirstSimple").outputs should contain theSameElementsAs Map("data" -> Set(
      thrift.Reference("ThirdSimple", "data1"),
      thrift.Reference("SecondSimple", "data")))
    graph("FirstSimple1").outputs should contain theSameElementsAs Map("data" -> Set(
      thrift.Reference("ThirdSimple", "data2")))
  }
}