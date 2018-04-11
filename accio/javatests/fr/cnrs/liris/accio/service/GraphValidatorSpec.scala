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

package fr.cnrs.liris.accio.service

import fr.cnrs.liris.accio.api.thrift.FieldViolation
import fr.cnrs.liris.accio.api.{Graph, OpRegistry, Values, thrift}
import fr.cnrs.liris.accio.testing.Operators
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[GraphValidator]].
 */
class GraphValidatorSpec extends UnitSpec {
  behavior of "GraphValidator"

  private val validator = new GraphValidator(new OpRegistry(Operators.ops))

  it should "validate a legitimate graph" in {
    val struct = Seq(
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
          "data" -> thrift.Input.Reference(thrift.Reference("FirstSimple", "data")))))
    val res = validator.validate(Graph.fromThrift(struct))
    res.isValid shouldBe true
    res.errors should have size 0
    res.warnings should have size 0
  }

  it should "detect duplicate node name" in {
    val struct = Seq(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42)))),
      thrift.Node(
        op = "SecondSimple",
        name = "FirstSimple",
        inputs = Map(
          "dbl" -> thrift.Input.Value(Values.encodeDouble(3.14)),
          "data" -> thrift.Input.Reference(thrift.Reference("FirstSimple", "data")))))
    val res = validator.validate(Graph.fromThrift(struct))
    res.isValid shouldBe false
    res.errors should contain(FieldViolation("Duplicate node name: FirstSimple", "graph"))
  }

  it should "detect unknown operator" in {
    val struct = Seq(thrift.Node(op = "InvalidOp", name = "MyOp"))
    val res = validator.validate(Graph.fromThrift(struct))
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Unknown operator: InvalidOp", "graph.0.op"))
  }

  it should "detect unknown input name" in {
    val struct = Seq(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map(
          "foo" -> thrift.Input.Value(Values.encodeInteger(42)),
          "bar" -> thrift.Input.Value(Values.encodeInteger(43)))))
    val res = validator.validate(Graph.fromThrift(struct))
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Unknown input for operator FirstSimple", "graph.0.inputs.bar"))
  }

  it should "detect invalid input type" in {
    val struct = Seq(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeString("bar")))))
    val res = validator.validate(Graph.fromThrift(struct))
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Data type mismatch: requires integer, got string", "graph.0.inputs.foo"))
  }

  it should "detect unknown input predecessor name" in {
    val struct = Seq(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42)))),
      thrift.Node(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> thrift.Input.Value(Values.encodeDouble(3.14)),
          "data" -> thrift.Input.Reference(thrift.Reference("UnknownTesting", "data")))))
    val res = validator.validate(Graph.fromThrift(struct))
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Reference to unknown node: UnknownTesting", "graph.1.inputs.data"))
  }

  it should "detect unknown input predecessor port" in {
    val struct = Seq(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42)))),
      thrift.Node(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> thrift.Input.Value(Values.encodeDouble(3.14)),
          "data" -> thrift.Input.Reference(thrift.Reference("FirstSimple", "unknown")))))
    val res = validator.validate(Graph.fromThrift(struct))
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Unknown output port for operator FirstSimple: FirstSimple/unknown", "graph.1.inputs.data"))
  }

  it should "detect missing roots" in {
    val struct = Seq(
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
          "data" -> thrift.Input.Reference(thrift.Reference("First", "data")))))
    val res = validator.validate(Graph.fromThrift(struct))
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("No root node", "graph"))
  }

  it should "detect inconsistent data type" in {
    val struct = Seq(
      thrift.Node(
        op = "FirstSimple",
        name = "First",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42)))),
      thrift.Node(
        op = "FirstSimple",
        name = "Second",
        inputs = Map("foo" -> thrift.Input.Reference(thrift.Reference("First", "data")))))
    val res = validator.validate(Graph.fromThrift(struct))
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Data type mismatch: requires integer, got dataset", "graph.1.inputs.foo"))
  }

  it should "detect missing input" in {
    val struct = Seq(thrift.Node(op = "FirstSimple", name = "FirstSimple"))
    val res = validator.validate(Graph.fromThrift(struct))
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Required input is missing: foo", "graph.0.inputs"))
  }

  it should "detect invalid node name" in {
    val struct = Seq(
      thrift.Node(
        op = "FirstSimple",
        name = "First/Simple",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42)))))
    val res = validator.validate(Graph.fromThrift(struct))
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Invalid node name: First/Simple (should match [A-Z][a-zA-Z0-9_]+)", "graph.0"))
  }

  it should "detect cycles" in {
    val struct = Seq(
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
          "data" -> thrift.Input.Reference(thrift.Reference("ThirdSimple", "data")))))
    val res = validator.validate(Graph.fromThrift(struct))
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Cycle detected: ThirdSimple -> SecondSimple -> ThirdSimple", "graph"))
  }

  it should "detect deprecated operators" in {
    val struct = Seq(
      thrift.Node(
        op = "Deprecated",
        name = "Deprecated",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42)))))
    val res = validator.validate(Graph.fromThrift(struct))
    res.warnings should contain(FieldViolation("Operator is deprecated: Do not use it!", "graph.0.op"))
  }
}