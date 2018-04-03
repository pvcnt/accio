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

package fr.cnrs.liris.accio.api

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[Workflow]].
 */
class WorkflowSpec extends UnitSpec {
  behavior of "Workflow"

  it should "create a workflow" in {
    val struct = thrift.Workflow(
      id = "my_workflow",
      graph = thrift.Graph(Seq(
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
      owner = Some(thrift.User("him")),
      params = Seq(
        thrift.ArgDef("foo", thrift.DataType(thrift.AtomicType.Integer)),
        thrift.ArgDef("bar", thrift.DataType(thrift.AtomicType.Double))))

    val workflow = Workflow.fromThrift(struct)
    workflow.name shouldBe "my_workflow"
    workflow.title shouldBe Some("my workflow")
    workflow.owner.map(_.name) shouldBe Some("him")
    workflow.graph should have size 2
    workflow.params should contain theSameElementsAs Set(
      thrift.ArgDef("foo", thrift.DataType(thrift.AtomicType.Integer)),
      thrift.ArgDef("bar", thrift.DataType(thrift.AtomicType.Double)))

    val workflowWithOwner = Workflow.fromThrift(struct, Some(UserInfo("me")))
    workflowWithOwner.owner.map(_.name) shouldBe Some("me")
  }
}