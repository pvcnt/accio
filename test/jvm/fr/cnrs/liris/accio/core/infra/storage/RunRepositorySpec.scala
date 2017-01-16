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
import fr.cnrs.liris.accio.testing.Runs
import fr.cnrs.liris.testing.UnitSpec

/**
 * Common unit tests for all [[RunRepository]] implementations, ensuring they all have consistent behavior.
 */
private[storage] abstract class RunRepositorySpec extends UnitSpec {
  protected def createRepository: RunRepository

  protected def refreshBeforeSearch(): Unit = {}

  it should "save and retrieve runs" in {
    val repo = createRepository
    repo.contains(Runs.Foobar.id) shouldBe false
    repo.get(Runs.Foobar.id) shouldBe None
    repo.contains(Runs.Foo.id) shouldBe false
    repo.get(Runs.Foo.id) shouldBe None

    repo.save(Runs.Foobar)
    refreshBeforeSearch()
    repo.contains(Runs.Foobar.id) shouldBe true
    repo.get(Runs.Foobar.id) shouldBe Some(Runs.Foobar)

    repo.save(Runs.Foo)
    refreshBeforeSearch()
    repo.get(Runs.Foo.id) shouldBe Some(Runs.Foo)
    repo.contains(Runs.Foo.id) shouldBe true
  }

  it should "delete runs" in {
    val repo = createRepository
    repo.save(Runs.Foobar)
    repo.contains(Runs.Foobar.id) shouldBe true
    repo.remove(Runs.Foobar.id)
    repo.contains(Runs.Foobar.id) shouldBe false
  }

  it should "search for runs" in {
    val repo = createRepository
    val runs = Seq(
      Runs.Foobar,
      Runs.Foobar.copy(id = Runs.randomId, createdAt = System.currentTimeMillis() + 10),
      Runs.Foobar.copy(id = Runs.randomId, createdAt = System.currentTimeMillis() + 40, owner = User("him")),
      Runs.Foobar.copy(id = Runs.randomId, createdAt = System.currentTimeMillis() + 50, pkg = Package(WorkflowId("other_workflow"), "v1")))
    runs.foreach(repo.save)
    refreshBeforeSearch()

    var res = repo.find(RunQuery(owner = Some("me")))
    res.totalCount shouldBe 3
    res.results should contain theSameElementsInOrderAs Seq(runs(3), runs(1), runs(0))

    res = repo.find(RunQuery(owner = Some("me"), limit = 2))
    res.totalCount shouldBe 3
    res.results should contain theSameElementsInOrderAs Seq(runs(3), runs(1))

    res = repo.find(RunQuery(owner = Some("me"), limit = 2, offset = Some(2)))
    res.totalCount shouldBe 3
    res.results should contain theSameElementsInOrderAs Seq(runs(0))

    res = repo.find(RunQuery(owner = Some("him")))
    res.totalCount shouldBe 1
    res.results should contain theSameElementsInOrderAs Seq(runs(2))

    res = repo.find(RunQuery(workflow = Some(WorkflowId("other_workflow"))))
    res.totalCount shouldBe 1
    res.results should contain theSameElementsInOrderAs Seq(runs(3))
  }
}