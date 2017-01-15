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

package fr.cnrs.liris.accio.core.infra.storage

import fr.cnrs.liris.accio.core.domain.{WorkflowId, _}
import fr.cnrs.liris.accio.testing.Workflows
import fr.cnrs.liris.testing.UnitSpec

/**
 * Common unit tests for all [[WorkflowRepository]] implementations, ensuring they all have consistent behavior.
 */
private[storage] abstract class WorkflowRepositorySpec extends UnitSpec {
  protected def createRepository: WorkflowRepository

  protected def refreshBeforeSearch(): Unit = {}

  it should "save and retrieve workflows" in {
    val repo = createRepository
    repo.contains(Workflows.workflow1.id) shouldBe false
    repo.get(Workflows.workflow1.id) shouldBe None
    repo.contains(Workflows.workflow2.id) shouldBe false
    repo.get(Workflows.workflow2.id) shouldBe None

    repo.save(Workflows.workflow1)
    refreshBeforeSearch()
    repo.contains(Workflows.workflow1.id) shouldBe true
    repo.get(Workflows.workflow1.id) shouldBe Some(Workflows.workflow1)

    repo.save(Workflows.workflow2)
    refreshBeforeSearch()
    repo.contains(Workflows.workflow2.id) shouldBe true
    repo.get(Workflows.workflow2.id) shouldBe Some(Workflows.workflow2)
  }

  it should "search for workflows" in {
    val repo = createRepository
    val workflows = Seq(
      Workflows.workflow1,
      Workflows.workflow1.copy(version = "v2"),
      Workflows.workflow2,
      Workflows.workflow2.copy(version = "v2"),
      Workflows.workflow1.copy(id = WorkflowId("other_workflow"), createdAt = System.currentTimeMillis() + 20),
      Workflows.workflow1.copy(id = WorkflowId("another_workflow"), createdAt = System.currentTimeMillis() + 30, owner = User("him")))
    workflows.foreach(repo.save)
    refreshBeforeSearch()

    var res = repo.find(WorkflowQuery(owner = Some("me")))
    res.totalCount shouldBe 3
    res.results should contain theSameElementsInOrderAs Seq(workflows(4), workflows(3), workflows(1))

    res = repo.find(WorkflowQuery(owner = Some("me"), limit = 2))
    res.totalCount shouldBe 3
    res.results should contain theSameElementsInOrderAs Seq(workflows(4), workflows(3))

    res = repo.find(WorkflowQuery(owner = Some("me"), limit = 2, offset = Some(2)))
    res.totalCount shouldBe 3
    res.results should contain theSameElementsInOrderAs Seq(workflows(1))
  }

  it should "check whether a workflow exists at a specific version" in {
    val repo = createRepository
    val workflows = Seq(
      Workflows.workflow1,
      Workflows.workflow1.copy(version = "v2"),
      Workflows.workflow2)
    workflows.foreach(repo.save)
    refreshBeforeSearch()

    repo.contains(Workflows.workflow1.id, "v1") shouldBe true
    repo.contains(Workflows.workflow1.id, "v2") shouldBe true
    repo.contains(Workflows.workflow1.id, "v3") shouldBe false
    repo.contains(Workflows.workflow2.id, "v1") shouldBe true
    repo.contains(Workflows.workflow2.id, "v2") shouldBe false
  }

  it should "retrieve a workflow at a specific version" in {
    val repo = createRepository
    val workflows = Seq(
      Workflows.workflow1,
      Workflows.workflow1.copy(version = "v2"),
      Workflows.workflow2)
    workflows.foreach(repo.save)
    refreshBeforeSearch()

    repo.get(Workflows.workflow1.id, "v1") shouldBe Some(workflows(0))
    repo.get(Workflows.workflow1.id, "v2") shouldBe Some(workflows(1))
    repo.get(Workflows.workflow1.id, "v3") shouldBe None
    repo.get(Workflows.workflow2.id, "v1") shouldBe Some(workflows(2))
    repo.get(Workflows.workflow2.id, "v2") shouldBe None
  }
}