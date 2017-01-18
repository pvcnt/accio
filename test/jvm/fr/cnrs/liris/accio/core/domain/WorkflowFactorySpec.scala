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

package fr.cnrs.liris.accio.core.domain

import fr.cnrs.liris.accio.testing.Operators
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[WorkflowFactory]].
 */
class WorkflowFactorySpec extends UnitSpec {
  private val workflow1 = WorkflowDef(
    id = WorkflowId("my_workflow"),
    graph = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Value(Values.encodeInteger(42)))),
      NodeDef(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> InputDef.Value(Values.encodeDouble(3.14)),
          "data" -> InputDef.Reference(Reference("FirstSimple", "data")))))),
    name = Some("my workflow"),
    owner = Some(User("him")))

  private val workflow2 = WorkflowDef(
    id = WorkflowId("workflow2"),
    params = Set(
      ArgDef("foo", DataType(AtomicType.Integer)),
      ArgDef("bar", DataType(AtomicType.Double))
    ),
    graph = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Param("foo"))),
      NodeDef(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> InputDef.Param("bar"),
          "data" -> InputDef.Reference(Reference("FirstSimple", "data")))))))

  private val workflow3 = WorkflowDef(
    id = WorkflowId("workflow3"),
    params = Set(
      ArgDef("foo", DataType(AtomicType.Integer), defaultValue = Some(Values.encodeInteger(42))),
      ArgDef("bar", DataType(AtomicType.Double)),
      ArgDef("string", DataType(AtomicType.String))
    ),
    graph = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Param("foo"))),
      NodeDef(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> InputDef.Param("bar"),
          "str" -> InputDef.Param("string"),
          "data" -> InputDef.Reference(Reference("FirstSimple", "data")))))))

  private val invalidParamNameWorkflow = WorkflowDef(
    id = WorkflowId("invalid_workflow"),
    params = Set(
      ArgDef("foo/foo", DataType(AtomicType.Integer))
    ),
    graph = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Param("foo/foo"))))))

  private val invalidParamTypeWorkflow = WorkflowDef(
    id = WorkflowId("invalid_workflow"),
    params = Set(ArgDef("foo", DataType(AtomicType.String))),
    graph = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Param("foo"))))))

  private val undeclaredParamWorkflow = WorkflowDef(
    id = WorkflowId("invalid_workflow"),
    graph = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Param("foo"))))))

  private val heterogeneousWorkflow = WorkflowDef(
    id = WorkflowId("invalid_workflow"),
    params = Set(ArgDef("foo", DataType(AtomicType.Integer))),
    graph = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Param("foo"))),
      NodeDef(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> InputDef.Param("foo"),
          "data" -> InputDef.Reference(Reference("FirstSimple", "data")))))))

  private val workflowFactory = {
    val opRegistry = new StaticOpRegistry(Operators.ops)
    val graphFactory = new GraphFactory(opRegistry)
    new WorkflowFactory(graphFactory, opRegistry)
  }

  behavior of "WorkflowFactory"

  it should "create a workflow" in {
    val workflow = workflowFactory.create(workflow1, User("me"))
    workflow.id shouldBe WorkflowId("my_workflow")
    workflow.name shouldBe Some("my workflow")
    workflow.isActive shouldBe true
    workflow.owner shouldBe User("him")
    workflow.graph shouldBe workflow1.graph
    workflow.params should have size 0
  }

  it should "populate default owner" in {
    val workflow = workflowFactory.create(workflow2, User("me"))
    workflow.owner shouldBe User("me")
  }

  it should "create a workflow with params" in {
    val workflow = workflowFactory.create(workflow2, User("me"))
    workflow.params should contain theSameElementsAs Set(
      ArgDef("foo", DataType(AtomicType.Integer)),
      ArgDef("bar", DataType(AtomicType.Double)))
  }

  it should "create a workflow with optional params" in {
    val workflow = workflowFactory.create(workflow3, User("me"))
    workflow.params should contain theSameElementsAs Set(
      ArgDef("foo", DataType(AtomicType.Integer), isOptional = true, defaultValue = Some(Values.encodeInteger(42))),
      ArgDef("bar", DataType(AtomicType.Double)),
      ArgDef("string", DataType(AtomicType.String), isOptional = true))
  }

  it should "detect param heterogeneous data types" in {
    val expected = intercept[InvalidWorkflowDefException] {
      workflowFactory.create(heterogeneousWorkflow, User("me"))
    }
    expected.getMessage shouldBe "Param foo is used with heterogeneous types: double, integer"
  }

  it should "detect invalid param type" in {
    val expected = intercept[InvalidWorkflowDefException] {
      workflowFactory.create(invalidParamTypeWorkflow, User("me"))
    }
    expected.getMessage should startWith("Param foo declared as string is used as integer")
  }

  it should "detect invalid param name" in {
    val expected = intercept[InvalidWorkflowDefException] {
      workflowFactory.create(invalidParamNameWorkflow, User("me"))
    }
    expected.getMessage should startWith("Illegal param name: foo/foo")
  }

  it should "detect undeclared param" in {
    val expected = intercept[InvalidWorkflowDefException] {
      workflowFactory.create(undeclaredParamWorkflow, User("me"))
    }
    expected.getMessage shouldBe "Some params are used but not declared: foo"
  }
}