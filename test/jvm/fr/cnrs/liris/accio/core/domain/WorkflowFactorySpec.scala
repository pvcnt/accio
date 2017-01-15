/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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
  private val workflows = Map(
    "workflow1" -> WorkflowTemplate(
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
      owner = Some(User("him"))),

    "workflow2" -> WorkflowTemplate(
      id = WorkflowId("workflow2"),
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
            "data" -> InputDef.Reference(Reference("FirstSimple", "data"))))))),

    "invalid_param_workflow" -> WorkflowTemplate(
      id = WorkflowId("invalid_param_workflow"),
      graph = GraphDef(Set(
        NodeDef(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Map("foo" -> InputDef.Param("foo/foo")))))),

    "heterogeneous_workflow" -> WorkflowTemplate(
      id = WorkflowId("heterogeneous_workflow"),
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
  )
  private val workflowFactory = {
    val opRegistry = new StaticOpRegistry(Operators.ops)
    val graphFactory = new GraphFactory(opRegistry)
    new WorkflowFactory(graphFactory, opRegistry)
  }

  behavior of "WorkflowFactory"

  it should "create a workflow" in {
    val workflow = workflowFactory.create(workflows("workflow1"), User("me"))
    workflow.id shouldBe WorkflowId("my_workflow")
    workflow.name shouldBe Some("my workflow")
    workflow.owner shouldBe User("him")
    workflow.graph shouldBe workflows("workflow1").graph
    workflow.params should have size 0
  }

  it should "populate default owner" in {
    val workflow = workflowFactory.create(workflows("workflow2"), User("me"))
    workflow.owner shouldBe User("me")
  }

  it should "collect implicit parameters" in {
    val workflow = workflowFactory.create(workflows("workflow2"), User("me"))
    workflow.params should contain theSameElementsAs Set(
      ArgDef("foo", None, DataType(AtomicType.Integer), isOptional = false),
      ArgDef("bar", None, DataType(AtomicType.Double), isOptional = false))
  }

  it should "detect heterogeneous data types" in {
    val expected = intercept[InvalidWorkflowException] {
      workflowFactory.create(workflows("heterogeneous_workflow"), User("me"))
    }
    expected.getMessage should startWith("Param foo is used in heterogeneous input types:")
  }

  it should "detect invalid param name" in {
    val expected = intercept[InvalidWorkflowException] {
      workflowFactory.create(workflows("invalid_param_workflow"), User("me"))
    }
    expected.getMessage should startWith("Invalid param name: foo/foo")
  }
}