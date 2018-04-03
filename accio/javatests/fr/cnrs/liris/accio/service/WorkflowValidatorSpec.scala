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
import fr.cnrs.liris.accio.api.{OpRegistry, Values, Workflow, thrift}
import fr.cnrs.liris.accio.testing.Operators
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[WorkflowValidator]].
 */
class WorkflowValidatorSpec extends UnitSpec {
  behavior of "WorkflowValidator"

  private[this] val validator = {
    val opRegistry = new OpRegistry(Operators.ops)
    val graphValidator = new GraphValidator(opRegistry)
    new WorkflowValidator(graphValidator, opRegistry)
  }

  it should "detect an invalid identifier" in {
    val struct = thrift.Workflow(
      id = "workflow!id",
      params = Seq(
        thrift.ArgDef("foo", thrift.DataType(thrift.AtomicType.Integer)),
        thrift.ArgDef("bar", thrift.DataType(thrift.AtomicType.Double))
      ),
      graph = thrift.Graph(Seq(
        thrift.Node(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Map("foo" -> thrift.Input.Param("foo"))),
        thrift.Node(
          op = "SecondSimple",
          name = "SecondSimple",
          inputs = Map(
            "dbl" -> thrift.Input.Param("bar"),
            "data" -> thrift.Input.Reference(thrift.Reference("FirstSimple", "data")))))))
    val res = validator.validate(Workflow.fromThrift(struct))
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Invalid workflow name: workflow!id (should match [a-zA-Z][a-zA-Z0-9_.-]+)", "name"))
  }

  it should "detect param heterogeneous data types" in {
    val struct = thrift.Workflow(
      id = "invalid_workflow",
      params = Seq(thrift.ArgDef("foo", thrift.DataType(thrift.AtomicType.Distance))),
      graph = thrift.Graph(Seq(
        thrift.Node(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Map("foo" -> thrift.Input.Param("foo"))),
        thrift.Node(
          op = "SecondSimple",
          name = "SecondSimple",
          inputs = Map(
            "dbl" -> thrift.Input.Param("foo"),
            "data" -> thrift.Input.Reference(thrift.Reference("FirstSimple", "data")))))))
    val res = validator.validate(Workflow.fromThrift(struct))
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(
      FieldViolation("Data type mismatch: requires integer, got distance", "graph.0.inputs.foo"),
      FieldViolation("Data type mismatch: requires double, got distance", "graph.1.inputs.dbl"))
  }

  it should "detect invalid param default value" in {
    val struct = thrift.Workflow(
      id = "invalid_workflow",
      params = Seq(thrift.ArgDef("foo", thrift.DataType(thrift.AtomicType.Integer), defaultValue = Some(Values.encodeString("barbar")))),
      graph = thrift.Graph(Seq(
        thrift.Node(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Map("foo" -> thrift.Input.Param("foo"))))))
    val res = validator.validate(Workflow.fromThrift(struct))
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Data type mismatch: requires integer, got string", "params.0.defaultValue"))
  }

  it should "detect invalid param name" in {
    val struct = thrift.Workflow(
      id = "invalid_workflow",
      params = Seq(
        thrift.ArgDef("foo/foo", thrift.DataType(thrift.AtomicType.Integer))
      ),
      graph = thrift.Graph(Seq(
        thrift.Node(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Map("foo" -> thrift.Input.Param("foo/foo"))))))
    val res = validator.validate(Workflow.fromThrift(struct))
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Invalid param name: foo/foo (should match [a-z][a-zA-Z0-9_]+)", "params.0.name"))
  }

  it should "detect undeclared param" in {
    val struct = thrift.Workflow(
      id = "invalid_workflow",
      graph = thrift.Graph(Seq(
        thrift.Node(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Map("foo" -> thrift.Input.Param("undeclared"))))))
    val res = validator.validate(Workflow.fromThrift(struct))
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Unknown param: undeclared", "graph.0.inputs.foo"))
  }
}