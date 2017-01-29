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

package fr.cnrs.liris.accio.core.runtime

import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.testing.Operators
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[GraphFactory]].
 */
class GraphFactorySpec extends UnitSpec {
  private val factory = {
    val opRegistry = new StaticOpRegistry(Operators.ops)
    new GraphFactory(opRegistry)
  }
  behavior of "GraphFactory"

  it should "populate node outputs" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Value(Values.encodeInteger(42)))),
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple1",
        inputs = Map("foo" -> InputDef.Value(Values.encodeInteger(42)))),
      NodeDef(
        op = "ThirdSimple",
        name = "ThirdSimple",
        inputs = Map(
          "data1" -> InputDef.Reference(Reference("FirstSimple", "data")),
          "data2" -> InputDef.Reference(Reference("FirstSimple1", "data")))),
      NodeDef(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> InputDef.Value(Values.encodeDouble(3.14)),
          "data" -> InputDef.Reference(Reference("FirstSimple", "data"))))
    ))
    val graph = factory.create(graphDef)

    graph("FirstSimple").outputs should contain theSameElementsAs Map("data" -> Set(
      Reference("ThirdSimple", "data1"),
      Reference("SecondSimple", "data")))
    graph("FirstSimple1").outputs should contain theSameElementsAs Map("data" -> Set(
      Reference("ThirdSimple", "data2")))
  }

  it should "detect duplicate node name" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Value(Values.encodeInteger(42)))),
      NodeDef(
        op = "SecondSimple",
        name = "FirstSimple",
        inputs = Map(
          "dbl" -> InputDef.Value(Values.encodeDouble(3.14)),
          "data" -> InputDef.Reference(Reference("FirstSimple", "data"))))
    ))
    assertErrors(graphDef, InvalidSpecMessage("Duplicate node name", Some("graph.FirstSimple")))
  }

  it should "detect unknown operator" in {
    val graphDef = GraphDef(Set(NodeDef(op = "InvalidOp", name = "MyOp")))
    assertErrors(graphDef, InvalidSpecMessage("Unknown operator: InvalidOp", Some("graph.MyOp.op")))
  }

  it should "detect unknown input name" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map(
          "foo" -> InputDef.Value(Values.encodeInteger(42)),
          "bar" -> InputDef.Value(Values.encodeInteger(43))))
    ))
    assertErrors(graphDef, InvalidSpecMessage("Unknown input port", Some("graph.FirstSimple.inputs.bar")))
  }

  it should "detect invalid input type" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Value(Values.encodeString("bar"))))
    ))
    assertErrors(graphDef, InvalidSpecMessage("Data type mismatch: requires integer, got string", Some("graph.FirstSimple.inputs.foo")))
  }

  it should "detect unknown input predecessor name" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Value(Values.encodeInteger(42)))),
      NodeDef(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> InputDef.Value(Values.encodeDouble(3.14)),
          "data" -> InputDef.Reference(Reference("UnknownTesting", "data"))))))
    assertErrors(graphDef, InvalidSpecMessage("Unknown node: UnknownTesting", Some("graph.SecondSimple.inputs.data")))
  }

  it should "detect unknown input predecessor port" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Value(Values.encodeInteger(42)))),
      NodeDef(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> InputDef.Value(Values.encodeDouble(3.14)),
          "data" -> InputDef.Reference(Reference("FirstSimple", "unknown"))))))
    assertErrors(graphDef, InvalidSpecMessage("Unknown output port: FirstSimple/unknown", Some("graph.SecondSimple.inputs.data")))
  }

  it should "detect missing roots" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "SecondSimple",
        name = "First",
        inputs = Map(
          "dbl" -> InputDef.Value(Values.encodeDouble(3.14)),
          "data" -> InputDef.Reference(Reference("Second", "data")))),
      NodeDef(
        op = "SecondSimple",
        name = "Second",
        inputs = Map(
          "dbl" -> InputDef.Value(Values.encodeDouble(3.14)),
          "data" -> InputDef.Reference(Reference("First", "data"))))))
    assertErrors(graphDef, InvalidSpecMessage("No root node"))
  }

  it should "detect inconsistent data type" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "First",
        inputs = Map("foo" -> InputDef.Value(Values.encodeInteger(42)))),
      NodeDef(
        op = "FirstSimple",
        name = "Second",
        inputs = Map("foo" -> InputDef.Reference(Reference("First", "data"))))))
    assertErrors(graphDef, InvalidSpecMessage("Data type mismatch: requires integer, got dataset", Some("graph.Second.inputs.foo")))
  }

  it should "detect missing input" in {
    val graphDef = GraphDef(Set(NodeDef(op = "FirstSimple", name = "FirstSimple")))
    assertErrors(graphDef, InvalidSpecMessage("No value for required input", Some("graph.FirstSimple.inputs.foo")))
  }

  it should "detect invalid node name" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "First/Simple",
        inputs = Map("foo" -> InputDef.Value(Values.encodeInteger(42))))))
    assertErrors(graphDef, InvalidSpecMessage("Invalid node name: First/Simple"))
  }

  it should "detect cycles" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Value(Values.encodeInteger(42)))),
      NodeDef(
        op = "ThirdSimple",
        name = "ThirdSimple",
        inputs = Map(
          "data1" -> InputDef.Reference(Reference("FirstSimple", "data")),
          "data2" -> InputDef.Reference(Reference("SecondSimple", "data")))),
      NodeDef(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> InputDef.Value(Values.encodeDouble(3.14)),
          "data" -> InputDef.Reference(Reference("ThirdSimple", "data"))))))
    assertErrors(graphDef, InvalidSpecMessage("Cycle detected: ThirdSimple -> SecondSimple -> ThirdSimple"))
  }

  private def assertErrors(graphDef: GraphDef, errors: InvalidSpecMessage*) = {
    val expected = intercept[InvalidSpecException] {
      factory.create(graphDef)
    }
    expected.errors should contain theSameElementsAs errors
  }
}