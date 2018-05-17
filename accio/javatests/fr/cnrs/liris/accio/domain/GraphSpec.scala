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

package fr.cnrs.liris.accio.domain

import fr.cnrs.liris.lumos.domain.Value
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[Graph]].
 */
class GraphSpec extends UnitSpec {
  behavior of "Graph"

  it should "create a graph" in {
    val steps = Seq(
      Step(
        op = "FirstSimple",
        name = "FirstSimple",
        params = Seq(Channel("foo", Channel.Constant(Value.Int(42))))),
      Step(
        op = "FirstSimple",
        name = "FirstSimple1",
        params = Seq(Channel("foo", Channel.Constant(Value.Int(42))))),
      Step(
        op = "ThirdSimple",
        name = "ThirdSimple",
        params = Seq(
          Channel("data1", Channel.Reference("FirstSimple", "data")),
          Channel("data2", Channel.Reference("FirstSimple1", "data")))),
      Step(
        op = "SecondSimple",
        name = "SecondSimple",
        params = Seq(
          Channel("dbl", Channel.Constant(Value.Double(3.14))),
          Channel("data", Channel.Reference("FirstSimple", "data")))))
    val graph = Graph.create(Workflow(steps = steps))

    graph.roots.map(_.name) should contain theSameElementsAs Set("FirstSimple", "FirstSimple1")
    graph("FirstSimple").isRoot shouldBe true
    graph("FirstSimple1").isRoot shouldBe true
    graph("ThirdSimple").isRoot shouldBe false
    graph("SecondSimple").isRoot shouldBe false

    graph("FirstSimple").successors should contain theSameElementsAs Set("ThirdSimple", "SecondSimple")
    graph("FirstSimple1").successors should contain theSameElementsAs Set("ThirdSimple")
    graph("ThirdSimple").successors should have size 0
    graph("SecondSimple").successors should have size 0

    graph("FirstSimple").predecessors should have size 0
    graph("FirstSimple1").predecessors should have size 0
    graph("ThirdSimple").predecessors should contain theSameElementsAs Set("FirstSimple", "FirstSimple1")
    graph("SecondSimple").predecessors should contain theSameElementsAs Set("FirstSimple")
  }
}