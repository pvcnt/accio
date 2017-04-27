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

import fr.cnrs.liris.accio.framework.api.thrift.{InvalidSpecException, InvalidSpecMessage}
import fr.cnrs.liris.accio.framework.api.thrift
import fr.cnrs.liris.accio.testing.Operators
import fr.cnrs.liris.dal.core.api.{AtomicType, DataType, Values}
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[WorkflowFactory]].
 */
class WorkflowFactorySpec extends UnitSpec {
  private[this] val factory = {
    val opRegistry = new StaticOpRegistry(Operators.ops)
    val graphFactory = new GraphFactory(opRegistry)
    new WorkflowFactory(graphFactory, opRegistry, new ValueValidator)
  }

  behavior of "thrift.WorkflowFactory"

  it should "create a workflow" in {
    val workflow = factory.create(Workflows.workflow1, thrift.User("me"))
    workflow.id shouldBe thrift.WorkflowId("my_workflow")
    workflow.name shouldBe Some("my workflow")
    workflow.isActive shouldBe true
    workflow.owner shouldBe Some(thrift.User("him"))
    workflow.graph shouldBe Workflows.workflow1.graph
    workflow.params should have size 0
  }

  it should "populate default owner" in {
    val workflow = factory.create(Workflows.workflow2, thrift.User("me"))
    workflow.owner shouldBe Some(thrift.User("me"))
  }

  it should "create a workflow with params" in {
    val workflow = factory.create(Workflows.workflow2, thrift.User("me"))
    workflow.params should contain theSameElementsAs Set(
      thrift.ArgDef("foo", DataType(AtomicType.Integer)),
      thrift.ArgDef("bar", DataType(AtomicType.Double)))
  }

  it should "create a workflow with optional params" in {
    val workflow = factory.create(Workflows.workflow3, thrift.User("me"))
    workflow.params should contain theSameElementsAs Set(
      thrift.ArgDef("foo", DataType(AtomicType.Integer), isOptional = true, defaultValue = Some(Values.encodeInteger(42))),
      thrift.ArgDef("bar", DataType(AtomicType.Double)),
      thrift.ArgDef("string", DataType(AtomicType.String), isOptional = true))
  }

  it should "detect an invalid identifier" in {
    assertErrors(
      Workflows.workflow2.copy(id = thrift.WorkflowId("workflow!id")),
      InvalidSpecMessage("Invalid workflow identifier: workflow!id (should match [a-zA-Z][a-zA-Z0-9_.-]+)", Some("id")))
  }

  it should "detect param heterogeneous data types" in {
    assertErrors(Workflows.heterogeneousWorkflow,
      InvalidSpecMessage("Param is used with heterogeneous types: double, integer", Some("params.foo")))
  }

  it should "detect invalid param type" in {
    assertErrors(
      Workflows.invalidParamTypeWorkflow,
      InvalidSpecMessage("Param declared as string is used as integer", Some("params.foo")))
  }

  it should "detect invalid param name" in {
    assertErrors(
      Workflows.invalidParamNameWorkflow,
      InvalidSpecMessage("Invalid param name: foo/foo (should match [a-z][a-zA-Z0-9_]+)"))
  }

  it should "detect undeclared param" in {
    assertErrors(
      Workflows.undeclaredParamWorkflow,
      InvalidSpecMessage("Param is not declared", Some("params.foo")))
  }

  it should "detect a param with invalid default value" in {
    // We only check a specific case here, to verify this is actually validated.
    // We otherwise rely on ValueValidatorSpec to test all edge cases.
    assertErrors(
      Workflows.invalidDefaultValueWorkflow,
      InvalidSpecMessage("Value must be <= 2000.0", Some("params.foo.default_value")))
  }

  private def assertErrors(spec: thrift.Workflow, errors: InvalidSpecMessage*) = {
    val expected = intercept[InvalidSpecException] {
      factory.create(spec, thrift.User("me"))
    }
    expected.errors should contain theSameElementsAs errors

    val result = factory.validate(spec)
    result.errors should contain theSameElementsAs errors
  }
}

object Workflows {
  val workflow1 = thrift.Workflow(
    id = thrift.WorkflowId("my_workflow"),
    isActive = true,
    graph = thrift.Graph(Set(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42)))),
      thrift.Node(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> thrift.Input.Value(Values.encodeDouble(3.14)),
          "data" -> thrift.Input.Reference(thrift.Reference("FirstSimple", "data")))))),
    name = Some("my workflow"),
    owner = Some(thrift.User("him")))

  val workflow2 = thrift.Workflow(
    id = thrift.WorkflowId("workflow2"),
    isActive = true,
    params = Set(
      thrift.ArgDef("foo", DataType(AtomicType.Integer)),
      thrift.ArgDef("bar", DataType(AtomicType.Double))
    ),
    graph = thrift.Graph(Set(
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

  val workflow3 = thrift.Workflow(
    id = thrift.WorkflowId("workflow3"),
    isActive = true,
    params = Set(
      thrift.ArgDef("foo", DataType(AtomicType.Integer), defaultValue = Some(Values.encodeInteger(42))),
      thrift.ArgDef("bar", DataType(AtomicType.Double)),
      thrift.ArgDef("string", DataType(AtomicType.String))
    ),
    graph = thrift.Graph(Set(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Param("foo"))),
      thrift.Node(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> thrift.Input.Param("bar"),
          "str" -> thrift.Input.Param("string"),
          "data" -> thrift.Input.Reference(thrift.Reference("FirstSimple", "data")))))))

  val invalidParamNameWorkflow = thrift.Workflow(
    id = thrift.WorkflowId("invalid_workflow"),
    isActive = true,
    params = Set(
      thrift.ArgDef("foo/foo", DataType(AtomicType.Integer))
    ),
    graph = thrift.Graph(Set(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Param("foo/foo"))))))

  val invalidParamTypeWorkflow = thrift.Workflow(
    id = thrift.WorkflowId("invalid_workflow"),
    isActive = true,
    params = Set(thrift.ArgDef("foo", DataType(AtomicType.String))),
    graph = thrift.Graph(Set(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Param("foo"))))))

  val invalidDefaultValueWorkflow = thrift.Workflow(
    id = thrift.WorkflowId("invalid_workflow"),
    isActive = true,
    params = Set(thrift.ArgDef("foo", DataType(AtomicType.Integer), defaultValue = Some(Values.encodeInteger(15008)))),
    graph = thrift.Graph(Set(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Param("foo"))))))

  val undeclaredParamWorkflow = thrift.Workflow(
    id = thrift.WorkflowId("invalid_workflow"),
    isActive = true,
    graph = thrift.Graph(Set(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Param("foo"))))))

  val heterogeneousWorkflow = thrift.Workflow(
    id = thrift.WorkflowId("invalid_workflow"),
    isActive = true,
    params = Set(thrift.ArgDef("foo", DataType(AtomicType.Integer))),
    graph = thrift.Graph(Set(
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
}