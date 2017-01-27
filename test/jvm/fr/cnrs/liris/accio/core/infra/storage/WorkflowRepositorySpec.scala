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

package fr.cnrs.liris.accio.core.infra.storage

import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.testing.UnitSpec

/**
 * Common unit tests for all [[MutableWorkflowRepository]] implementations, ensuring they all have consistent behavior.
 */
private[storage] abstract class WorkflowRepositorySpec extends UnitSpec {
  private[this] val workflow1 = Workflow(
    id = WorkflowId("workflow1"),
    version = "v1",
    owner = User("me"),
    isActive = true,
    createdAt = System.currentTimeMillis(),
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
    name = Some("my workflow"))

  private[this] val workflow2 = Workflow(
    id = WorkflowId("workflow2"),
    version = "v1",
    owner = User("me"),
    isActive = true,
    params = Set(ArgDef("foo", DataType(AtomicType.Integer))),
    createdAt = System.currentTimeMillis() + 10,
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

  protected def createRepository: MutableWorkflowRepository

  protected def refreshBeforeSearch(): Unit = {}

  it should "save and retrieve workflows" in {
    val repo = createRepository
    repo.contains(workflow1.id) shouldBe false
    repo.get(workflow1.id) shouldBe None
    repo.contains(workflow2.id) shouldBe false
    repo.get(workflow2.id) shouldBe None

    repo.save(workflow1)
    refreshBeforeSearch()
    repo.contains(workflow1.id) shouldBe true
    repo.get(workflow1.id) shouldBe Some(workflow1)

    repo.save(workflow2)
    refreshBeforeSearch()
    repo.contains(workflow2.id) shouldBe true
    repo.get(workflow2.id) shouldBe Some(workflow2)
  }

  it should "search for workflows" in {
    val repo = createRepository
    val workflows = Seq(
      workflow1,
      workflow1.copy(version = "v2"),
      workflow2,
      workflow2.copy(version = "v2"),
      workflow1.copy(id = WorkflowId("other_workflow"), createdAt = System.currentTimeMillis() + 20),
      workflow1.copy(id = WorkflowId("another_workflow"), createdAt = System.currentTimeMillis() + 30, owner = User("him")))
    workflows.foreach(repo.save)
    refreshBeforeSearch()

    var res = repo.find(WorkflowQuery(owner = Some("me")))
    res.totalCount shouldBe 3
    res.results should contain theSameElementsInOrderAs Seq(workflows(4), workflows(3), workflows(1)).map(unsetNodes)

    res = repo.find(WorkflowQuery(owner = Some("me"), limit = 2))
    res.totalCount shouldBe 3
    res.results should contain theSameElementsInOrderAs Seq(workflows(4), workflows(3)).map(unsetNodes)

    res = repo.find(WorkflowQuery(owner = Some("me"), limit = 2, offset = Some(2)))
    res.totalCount shouldBe 3
    res.results should contain theSameElementsInOrderAs Seq(workflows(1)).map(unsetNodes)
  }

  it should "check whether a workflow exists at a specific version" in {
    val repo = createRepository
    val workflows = Seq(
      workflow1,
      workflow1.copy(version = "v2"),
      workflow2)
    workflows.foreach(repo.save)
    refreshBeforeSearch()

    repo.contains(workflow1.id, "v1") shouldBe true
    repo.contains(workflow1.id, "v2") shouldBe true
    repo.contains(workflow1.id, "v3") shouldBe false
    repo.contains(workflow2.id, "v1") shouldBe true
    repo.contains(workflow2.id, "v2") shouldBe false
  }

  it should "retrieve a workflow at a specific version" in {
    val repo = createRepository
    val workflows = Seq(
      workflow1,
      workflow1.copy(version = "v2"),
      workflow2)
    workflows.foreach(repo.save)
    refreshBeforeSearch()

    repo.get(workflow1.id, "v1") shouldBe Some(workflows(0).copy(isActive = false))
    repo.get(workflow1.id, "v2") shouldBe Some(workflows(1))
    repo.get(workflow1.id, "v3") shouldBe None
    repo.get(workflow2.id, "v1") shouldBe Some(workflows(2))
    repo.get(workflow2.id, "v2") shouldBe None
  }

  private def unsetNodes(workflow: Workflow) = workflow.copy(graph = workflow.graph.unsetNodes)
}