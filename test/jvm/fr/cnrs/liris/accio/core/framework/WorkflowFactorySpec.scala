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

package fr.cnrs.liris.accio.core.framework

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[WorkflowFactory]].
 */
class WorkflowFactorySpec extends UnitSpec {
  val workflows = Map(
    "workflow1" -> WorkflowDef(
      id = Some("my_workflow"),
      graph = GraphDef(Seq(
        NodeDef(
          op = "FirstSimple",
          inputs = Map("foo" -> ValueInput(42))),
        NodeDef(
          op = "SecondSimple",
          inputs = Map(
            "dbl" -> ValueInput(3.14),
            "data" -> ReferenceInput(Reference("FirstSimple", "data")))))),
      name = Some("my workflow"),
      owner = Some(User("him"))),

    "workflow2" -> WorkflowDef(
      id = Some("workflow2"),
      graph = GraphDef(Seq(
        NodeDef(
          op = "FirstSimple",
          inputs = Map("foo" -> ParamInput("foo"))),
        NodeDef(
          op = "SecondSimple",
          inputs = Map(
            "dbl" -> ParamInput("bar"),
            "data" -> ReferenceInput(Reference("FirstSimple", "data"))))))),

    "invalid_param_workflow" -> WorkflowDef(
      id = Some("invalid_param_workflow"),
      graph = GraphDef(Seq(
        NodeDef(
          op = "FirstSimple",
          inputs = Map("foo" -> ParamInput("foo/foo")))))),

    "/path/to/some_workflow.json" -> WorkflowDef(
      graph = GraphDef(Seq(
        NodeDef(
          op = "FirstSimple",
          inputs = Map("foo" -> ValueInput(42)))))),

    "heterogeneous_workflow" -> WorkflowDef(
      id = Some("heterogeneous_workflow"),
      graph = GraphDef(Seq(
        NodeDef(
          op = "FirstSimple",
          inputs = Map("foo" -> ParamInput("foo"))),
        NodeDef(
          op = "SecondSimple",
          inputs = Map(
            "dbl" -> ParamInput("foo"),
            "data" -> ReferenceInput(Reference("FirstSimple", "data")))))))
  )

  val graphFactory = {
    val reader = new ReflectOpMetaReader
    val opRegistry = new OpRegistry(reader, Set(classOf[FirstSimpleOp], classOf[SecondSimpleOp], classOf[ThirdSimpleOp]))
    new GraphFactory(opRegistry)
  }

  val workflowFactory = {
    val reader = new ReflectOpMetaReader
    val opRegistry = new OpRegistry(reader, Set(classOf[FirstSimpleOp], classOf[SecondSimpleOp], classOf[ThirdSimpleOp]))
    val graphFactory = new GraphFactory(opRegistry)
    val parser = new DummyWorkflowParser(workflows)
    new WorkflowFactory(parser, graphFactory, opRegistry)
  }

  behavior of "WorkflowFactory"

  it should "create a workflow" in {
    val workflow = workflowFactory.create("workflow1", User("me"))
    workflow.graph shouldBe graphFactory.create(workflows("workflow1").graph)
    workflow.id shouldBe "my_workflow"
    workflow.name shouldBe Some("my workflow")
    workflow.owner shouldBe User("him")
    workflow.params should have size 0
  }

  it should "fill an empty owner" in {
    val workflow = workflowFactory.create("/path/to/some_workflow.json", User("me"))
    workflow.owner shouldBe User("me")
  }

  it should "fill an empty identifier" in {
    val workflow = workflowFactory.create("/path/to/some_workflow.json", User("me"))
    workflow.id shouldBe "some_workflow"
  }

  it should "collect parameters" in {
    val workflow = workflowFactory.create("workflow2", User("me"))
    workflow.params should contain theSameElementsAs Set(
      Param("foo", DataType.Integer, isOptional = false, ports = Set(Reference("FirstSimple", "foo"))),
      Param("bar", DataType.Double, isOptional = false, ports = Set(Reference("SecondSimple", "dbl"))))
  }

  it should "detect heterogeneous data types" in {
    val expected = intercept[IllegalWorkflowException] {
      workflowFactory.create("heterogeneous_workflow", User("me"))
    }
    expected.getMessage should startWith("Param foo is used in heterogeneous input types:")
  }

  it should "detect invalid param name" in {
    val expected = intercept[IllegalWorkflowException] {
      workflowFactory.create("invalid_param_workflow", User("me"))
    }
    expected.getMessage should startWith("Invalid param name: foo/foo (must match ")
  }
}

private[framework] class DummyWorkflowParser(workflows: Map[String, WorkflowDef]) extends WorkflowParser {
  override def parse(uri: String): WorkflowDef = workflows(uri)

  override def canRead(uri: String): Boolean = workflows.contains(uri)
}