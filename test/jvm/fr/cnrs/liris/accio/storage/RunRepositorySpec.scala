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

package fr.cnrs.liris.accio.storage

import java.util.UUID

import fr.cnrs.liris.accio.api.Values
import fr.cnrs.liris.accio.api.thrift._

import scala.collection.Map

/**
 * Common unit tests for all [[MutableRunRepository]] implementations, ensuring they all have consistent behavior.
 */
private[storage] abstract class RunRepositorySpec extends RepositorySpec[MutableRunRepository] {
  protected val foobarRun = Run(
    id = RunId("foobar"),
    pkg = Package(WorkflowId("my_workflow"), "v1"),
    cluster = "default",
    owner = User("me"),
    name = Some("foo bar workflow"),
    notes = Some("awesome workflow!"),
    tags = Set("foo", "bar"),
    seed = 1234,
    params = Map.empty,
    createdAt = System.currentTimeMillis(),
    state = RunStatus(status = TaskState.Scheduled, progress = 0))

  protected val fooRun = Run(
    id = RunId("foo"),
    pkg = Package(WorkflowId("my_workflow"), "v1"),
    cluster = "default",
    owner = User("me"),
    name = Some("foo bar workflow"),
    tags = Set("foo"),
    seed = 54321,
    params = Map.empty,
    createdAt = System.currentTimeMillis() - 1000,
    state = RunStatus(status = TaskState.Running, progress = .5))

  private val runs = Seq(
    foobarRun,
    foobarRun.copy(
      id = randomId,
      createdAt = System.currentTimeMillis() + 10,
      state = foobarRun.state.copy(status = TaskState.Running),
      tags = Set("foo")),
    foobarRun.copy(
      id = randomId,
      createdAt = System.currentTimeMillis() + 40,
      state = foobarRun.state.copy(status = TaskState.Running),
      owner = User("him"),
      tags = Set("foobar")),
    foobarRun.copy(
      id = randomId,
      createdAt = System.currentTimeMillis() + 50,
      pkg = Package(WorkflowId("other_workflow"), "v1")),
    foobarRun.copy(
      id = randomId,
      createdAt = System.currentTimeMillis() + 60,
      parent = Some(foobarRun.id),
      tags = Set.empty))

  protected val foobarResults = Map(
    "FooNode" -> OpResult(
      0,
      None,
      Set(Artifact("myint", Values.encodeInteger(42)), Artifact("mystr", Values.encodeString("foo str"))),
      Set(Metric("a", 1), Metric("b", 2))),
    "BarNode" -> OpResult(
      0,
      None,
      Set(Artifact("dbl", Values.encodeDouble(3.14))),
      Set(Metric("a", 12))))

  protected val fooResults = Map("FooNode" -> OpResult(
    0,
    None,
    Set(Artifact("myint", Values.encodeInteger(44)), Artifact("mystr", Values.encodeString("str"))),
    Set(Metric("a", 3), Metric("b", 4))))

  protected def refreshBeforeSearch(): Unit = {}

  it should "save and retrieve runs" in {
    repo.get(foobarRun.id) shouldBe None
    repo.get(fooRun.id) shouldBe None

    repo.save(foobarRun)
    refreshBeforeSearch()
    repo.get(foobarRun.id) shouldBe Some(foobarRun)

    repo.save(fooRun)
    refreshBeforeSearch()
    repo.get(fooRun.id) shouldBe Some(fooRun)
  }

  it should "delete runs" in {
    repo.save(foobarRun)
    repo.save(fooRun)
    refreshBeforeSearch()

    repo.remove(foobarRun.id)
    refreshBeforeSearch()
    repo.get(fooRun.id) shouldBe Some(fooRun)
    repo.get(foobarRun.id) shouldBe None
  }

  it should "search for runs by owner" in {
    runs.foreach(repo.save)
    refreshBeforeSearch()

    var res = repo.find(RunQuery(owner = Some("me")))
    res.totalCount shouldBe 3
    res.results should contain theSameElementsInOrderAs Seq(runs(3), runs(1), runs(0)).map(unsetResult)

    res = repo.find(RunQuery(owner = Some("me"), limit = Some(2)))
    res.totalCount shouldBe 3
    res.results should contain theSameElementsInOrderAs Seq(runs(3), runs(1)).map(unsetResult)

    res = repo.find(RunQuery(owner = Some("me"), limit = Some(2), offset = Some(2)))
    res.totalCount shouldBe 3
    res.results should contain theSameElementsInOrderAs Seq(runs(0)).map(unsetResult)

    res = repo.find(RunQuery(owner = Some("him")))
    res.totalCount shouldBe 1
    res.results should contain theSameElementsInOrderAs Seq(runs(2)).map(unsetResult)
  }

  it should "search for runs by workflow" in {
    runs.foreach(repo.save)
    refreshBeforeSearch()

    val res = repo.find(RunQuery(workflow = Some(WorkflowId("other_workflow"))))
    res.totalCount shouldBe 1
    res.results should contain theSameElementsInOrderAs Seq(runs(3)).map(unsetResult)
  }


  it should "search for runs by status" in {
    runs.foreach(repo.save)
    refreshBeforeSearch()

    val res = repo.find(RunQuery(status = Set(TaskState.Running)))
    res.totalCount shouldBe 2
    res.results should contain theSameElementsInOrderAs Seq(runs(2), runs(1)).map(unsetResult)
  }


  it should "search for runs by tags" in {
    runs.foreach(repo.save)
    refreshBeforeSearch()

    var res = repo.find(RunQuery(tags = Set("foo", "bar")))
    res.totalCount shouldBe 2
    res.results should contain theSameElementsInOrderAs Seq(runs(3), runs(0)).map(unsetResult)

    res = repo.find(RunQuery(tags = Set("foo")))
    res.totalCount shouldBe 3
    res.results should contain theSameElementsInOrderAs Seq(runs(3), runs(1), runs(0)).map(unsetResult)

    res = repo.find(RunQuery(tags = Set("foobar")))
    res.totalCount shouldBe 1
    res.results should contain theSameElementsInOrderAs Seq(runs(2)).map(unsetResult)
  }

  private def unsetResult(run: Run) = run.copy(state = run.state.copy(nodes = run.state.nodes.map(_.unsetResult)))

  private def randomId: RunId = RunId(UUID.randomUUID().toString)
}

private[storage] trait RunRepositorySpecWithMemoization extends RunRepositorySpec {
  private val foobarRunWithNodes = foobarRun.copy(state = foobarRun.state.copy(nodes = Set(
    NodeStatus(name = "FooNode", status = TaskState.Success, cacheKey = Some(CacheKey("MyFooCacheKey")), result = Some(foobarResults("FooNode"))),
    NodeStatus(name = "BarNode", status = TaskState.Success, cacheKey = Some(CacheKey("MyBarCacheKey")), result = Some(foobarResults("BarNode")))
  )))
  private val fooRunWithNodes = fooRun.copy(state = fooRun.state.copy(nodes = Set(
    NodeStatus(name = "FooNode", status = TaskState.Success, cacheKey = Some(CacheKey("YourFooCacheKey")), result = Some(fooResults("FooNode"))))))

  it should "memoize artifacts" in {
    repo.save(foobarRunWithNodes)
    repo.save(fooRunWithNodes)
    refreshBeforeSearch()

    repo.get(CacheKey("MyFooCacheKey")) shouldBe Some(foobarResults("FooNode"))
    repo.get(CacheKey("MyBarCacheKey")) shouldBe Some(foobarResults("BarNode"))
    repo.get(CacheKey("YourFooCacheKey")) shouldBe Some(fooResults("FooNode"))
    repo.get(CacheKey("UnknownKey")) shouldBe None
  }
}