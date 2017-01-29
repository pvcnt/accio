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
 * Unit tests for [[WorkflowFactory]].
 */
class WorkflowFactorySpec extends UnitSpec {
  private[this] val factory = {
    val opRegistry = new StaticOpRegistry(Operators.ops)
    val graphFactory = new GraphFactory(opRegistry)
    new WorkflowFactory(graphFactory, opRegistry)
  }

  behavior of "WorkflowFactory"

  it should "create a workflow" in {
    val workflow = factory.create(Workflows.workflow1, User("me"))
    workflow.id shouldBe WorkflowId("my_workflow")
    workflow.name shouldBe Some("my workflow")
    workflow.isActive shouldBe true
    workflow.owner shouldBe User("him")
    workflow.graph shouldBe Workflows.workflow1.graph
    workflow.params should have size 0
  }

  it should "populate default owner" in {
    val workflow = factory.create(Workflows.workflow2, User("me"))
    workflow.owner shouldBe User("me")
  }

  it should "create a workflow with params" in {
    val workflow = factory.create(Workflows.workflow2, User("me"))
    workflow.params should contain theSameElementsAs Set(
      ArgDef("foo", DataType(AtomicType.Integer)),
      ArgDef("bar", DataType(AtomicType.Double)))
  }

  it should "create a workflow with optional params" in {
    val workflow = factory.create(Workflows.workflow3, User("me"))
    workflow.params should contain theSameElementsAs Set(
      ArgDef("foo", DataType(AtomicType.Integer), isOptional = true, defaultValue = Some(Values.encodeInteger(42))),
      ArgDef("bar", DataType(AtomicType.Double)),
      ArgDef("string", DataType(AtomicType.String), isOptional = true))
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
      InvalidSpecMessage("Invalid param name: foo/foo"))
  }

  it should "detect undeclared param" in {
    assertErrors(
      Workflows.undeclaredParamWorkflow,
      InvalidSpecMessage("Param is not declared", Some("params.foo")))
  }

  private def assertErrors(spec: WorkflowSpec, errors: InvalidSpecMessage*) = {
    val expected = intercept[InvalidSpecException] {
      factory.create(spec, User("me"))
    }
    expected.errors should contain theSameElementsAs errors

    val result = factory.validate(spec)
    result.errors should contain theSameElementsAs errors
  }
}

object Workflows {
  val workflow1 = WorkflowSpec(
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

  val workflow2 = WorkflowSpec(
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

  val workflow3 = WorkflowSpec(
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

   val invalidParamNameWorkflow = WorkflowSpec(
    id = WorkflowId("invalid_workflow"),
    params = Set(
      ArgDef("foo/foo", DataType(AtomicType.Integer))
    ),
    graph = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Param("foo/foo"))))))

   val invalidParamTypeWorkflow = WorkflowSpec(
    id = WorkflowId("invalid_workflow"),
    params = Set(ArgDef("foo", DataType(AtomicType.String))),
    graph = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Param("foo"))))))

   val undeclaredParamWorkflow = WorkflowSpec(
    id = WorkflowId("invalid_workflow"),
    graph = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Param("foo"))))))

   val heterogeneousWorkflow = WorkflowSpec(
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
}