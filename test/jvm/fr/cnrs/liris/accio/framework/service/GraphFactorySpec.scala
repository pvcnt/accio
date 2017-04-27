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

package fr.cnrs.liris.accio.framework.service

import fr.cnrs.liris.accio.framework.api.thrift
import fr.cnrs.liris.accio.framework.api.thrift.{InvalidSpecException, InvalidSpecMessage}
import fr.cnrs.liris.accio.testing.Operators
import fr.cnrs.liris.dal.core.api.Values
import fr.cnrs.liris.testing.UnitSpec

import scala.collection.mutable

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
    val struct = thrift.Graph(Set(
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
    val graph = factory.create(struct)

    graph("FirstSimple").outputs should contain theSameElementsAs Map("data" -> Set(
      thrift.Reference("ThirdSimple", "data1"),
      thrift.Reference("SecondSimple", "data")))
    graph("FirstSimple1").outputs should contain theSameElementsAs Map("data" -> Set(
      thrift.Reference("ThirdSimple", "data2")))
  }

  it should "detect duplicate node name" in {
    val struct = thrift.Graph(Set(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42)))),
      thrift.Node(
        op = "SecondSimple",
        name = "FirstSimple",
        inputs = Map(
          "dbl" -> thrift.Input.Value(Values.encodeDouble(3.14)),
          "data" -> thrift.Input.Reference(thrift.Reference("FirstSimple", "data"))))
    ))
    assertErrors(struct, InvalidSpecMessage("Duplicate node name", Some("graph.FirstSimple")))
  }

  it should "detect unknown operator" in {
    val struct = thrift.Graph(Set(thrift.Node(op = "InvalidOp", name = "MyOp")))
    assertErrors(struct, InvalidSpecMessage("Unknown operator: InvalidOp", Some("graph.MyOp.op")))
  }

  it should "detect unknown input name" in {
    val struct = thrift.Graph(Set(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map(
          "foo" -> thrift.Input.Value(Values.encodeInteger(42)),
          "bar" -> thrift.Input.Value(Values.encodeInteger(43))))
    ))
    assertErrors(struct, InvalidSpecMessage("Unknown input port", Some("graph.FirstSimple.inputs.bar")))
  }

  it should "detect invalid input type" in {
    val struct = thrift.Graph(Set(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeString("bar"))))
    ))
    assertErrors(struct, InvalidSpecMessage("Data type mismatch: requires integer, got string", Some("graph.FirstSimple.inputs.foo")))
  }

  it should "detect unknown input predecessor name" in {
    val struct = thrift.Graph(Set(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42)))),
      thrift.Node(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> thrift.Input.Value(Values.encodeDouble(3.14)),
          "data" -> thrift.Input.Reference(thrift.Reference("UnknownTesting", "data"))))))
    assertErrors(struct, InvalidSpecMessage("Unknown node: UnknownTesting", Some("graph.SecondSimple.inputs.data")))
  }

  it should "detect unknown input predecessor port" in {
    val struct = thrift.Graph(Set(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42)))),
      thrift.Node(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> thrift.Input.Value(Values.encodeDouble(3.14)),
          "data" -> thrift.Input.Reference(thrift.Reference("FirstSimple", "unknown"))))))
    assertErrors(struct, InvalidSpecMessage("Unknown output port: FirstSimple/unknown", Some("graph.SecondSimple.inputs.data")))
  }

  it should "detect missing roots" in {
    val struct = thrift.Graph(Set(
      thrift.Node(
        op = "SecondSimple",
        name = "First",
        inputs = Map(
          "dbl" -> thrift.Input.Value(Values.encodeDouble(3.14)),
          "data" -> thrift.Input.Reference(thrift.Reference("Second", "data")))),
      thrift.Node(
        op = "SecondSimple",
        name = "Second",
        inputs = Map(
          "dbl" -> thrift.Input.Value(Values.encodeDouble(3.14)),
          "data" -> thrift.Input.Reference(thrift.Reference("First", "data"))))))
    assertErrors(struct, InvalidSpecMessage("No root node"))
  }

  it should "detect inconsistent data type" in {
    val struct = thrift.Graph(Set(
      thrift.Node(
        op = "FirstSimple",
        name = "First",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42)))),
      thrift.Node(
        op = "FirstSimple",
        name = "Second",
        inputs = Map("foo" -> thrift.Input.Reference(thrift.Reference("First", "data"))))))
    assertErrors(struct, InvalidSpecMessage("Data type mismatch: requires integer, got dataset", Some("graph.Second.inputs.foo")))
  }

  it should "detect missing input" in {
    val struct = thrift.Graph(Set(thrift.Node(op = "FirstSimple", name = "FirstSimple")))
    assertErrors(struct, InvalidSpecMessage("No value for required input", Some("graph.FirstSimple.inputs.foo")))
  }

  it should "detect invalid node name" in {
    val struct = thrift.Graph(Set(
      thrift.Node(
        op = "FirstSimple",
        name = "First/Simple",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42))))))
    assertErrors(struct, InvalidSpecMessage("Invalid node name: First/Simple (should match [A-Z][a-zA-Z0-9_]+)"))
  }

  it should "detect cycles" in {
    val struct = thrift.Graph(Set(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42)))),
      thrift.Node(
        op = "ThirdSimple",
        name = "ThirdSimple",
        inputs = Map(
          "data1" -> thrift.Input.Reference(thrift.Reference("FirstSimple", "data")),
          "data2" -> thrift.Input.Reference(thrift.Reference("SecondSimple", "data")))),
      thrift.Node(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> thrift.Input.Value(Values.encodeDouble(3.14)),
          "data" -> thrift.Input.Reference(thrift.Reference("ThirdSimple", "data"))))))
    assertErrors(struct, InvalidSpecMessage("Cycle detected: ThirdSimple -> SecondSimple -> ThirdSimple"))
  }

  it should "detect deprecated operators" in {
    val struct = thrift.Graph(Set(
      thrift.Node(
        op = "Deprecated",
        name = "Deprecated",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42))))))
    val warnings = mutable.Set.empty[InvalidSpecMessage]
    factory.create(struct, warnings)
    warnings should contain(InvalidSpecMessage("Operator is deprecated: Do not use it!", Some("graph.Deprecated")))
  }

  private def assertErrors(struct: thrift.Graph, errors: InvalidSpecMessage*) = {
    val expected = intercept[InvalidSpecException] {
      factory.create(struct)
    }
    expected.errors should contain theSameElementsAs errors
  }
}