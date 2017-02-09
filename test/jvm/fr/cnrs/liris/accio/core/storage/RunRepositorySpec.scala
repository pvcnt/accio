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

package fr.cnrs.liris.accio.core.storage

import java.util.UUID

import com.twitter.util.Time
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.dal.core.api.Values
import fr.cnrs.liris.testing.UnitSpec

import scala.collection.Map

/**
 * Common unit tests for all [[MutableRunRepository]] implementations, ensuring they all have consistent behavior.
 */
private[storage] abstract class RunRepositorySpec extends UnitSpec {
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
    state = RunState(status = RunStatus.Scheduled, progress = 0))

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
    state = RunState(status = RunStatus.Running, progress = .5))

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
  protected val foobarRunWithNodes = foobarRun.copy(state = foobarRun.state.copy(nodes = Set(
    NodeState(name = "FooNode", status = NodeStatus.Success, cacheKey = Some(CacheKey("MyFooCacheKey")), result = Some(foobarResults("FooNode"))),
    NodeState(name = "BarNode", status = NodeStatus.Success, cacheKey = Some(CacheKey("MyBarCacheKey")), result = Some(foobarResults("BarNode")))
  )))
  protected val fooResults = Map("FooNode" -> OpResult(
    0,
    None,
    Set(Artifact("myint", Values.encodeInteger(44)), Artifact("mystr", Values.encodeString("str"))),
    Set(Metric("a", 3), Metric("b", 4))))
  protected val fooRunWithNodes = fooRun.copy(state = fooRun.state.copy(nodes = Set(
    NodeState(name = "FooNode", status = NodeStatus.Success, cacheKey = Some(CacheKey("YourFooCacheKey")), result = Some(fooResults("FooNode"))))))

  protected def createRepository: MutableRunRepository

  protected def refreshBeforeSearch(): Unit = {}

  it should "save and retrieve runs" in {
    val repo = createRepository
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
    val repo = createRepository
    repo.save(foobarRun)
    repo.save(fooRun)
    refreshBeforeSearch()

    repo.remove(foobarRun.id)
    refreshBeforeSearch()
    repo.get(fooRun.id) shouldBe Some(fooRun)
    repo.get(foobarRun.id) shouldBe None
  }

  it should "search for runs" in {
    val repo = createRepository
    val runs = Seq(
      foobarRun,
      foobarRun.copy(id = randomId, createdAt = System.currentTimeMillis() + 10, state = foobarRun.state.copy(status = RunStatus.Running)),
      foobarRun.copy(id = randomId, createdAt = System.currentTimeMillis() + 40, state = foobarRun.state.copy(status = RunStatus.Running), owner = User("him")),
      foobarRun.copy(id = randomId, createdAt = System.currentTimeMillis() + 50, pkg = Package(WorkflowId("other_workflow"), "v1")),
      foobarRun.copy(id = randomId, parent = Some(foobarRun.id)))
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

    res = repo.find(RunQuery(workflow = Some(WorkflowId("other_workflow"))))
    res.totalCount shouldBe 1
    res.results should contain theSameElementsInOrderAs Seq(runs(3)).map(unsetResult)

    res = repo.find(RunQuery(status = Set(RunStatus.Running)))
    res.totalCount shouldBe 2
    res.results should contain theSameElementsInOrderAs Seq(runs(2), runs(1)).map(unsetResult)
  }

  it should "save logs" in {
    val repo = createRepository
    val runIds = Seq.fill(5)(randomId)
    val now = System.currentTimeMillis()
    val logs = runIds.map { runId =>
      runId -> Seq.tabulate(3) { i =>
        s"Node$i" -> (Seq.tabulate(10) { j =>
          RunLog(runId, s"Node$i", now + i * 25 + j, "stdout", s"line $i $j")
        } ++ Seq.tabulate(15) { j =>
          RunLog(runId, s"Node$i", now + i * 25 + 10 + j, "stderr", s"line $i $j")
        })
      }.toMap
    }.toMap
    repo.save(logs.values.flatMap(_.values).flatten.toSeq)
    refreshBeforeSearch()

    var res = repo.find(LogsQuery(runIds.head, "Node2"))
    res should contain theSameElementsAs logs(runIds.head)("Node2")

    res = repo.find(LogsQuery(runIds.head, "Node2", classifier = Some("stdout")))
    res should contain theSameElementsAs logs(runIds.head)("Node2").take(10)

    res = repo.find(LogsQuery(runIds.head, "Node2", limit = Some(10)))
    res should have size 10

    res = repo.find(LogsQuery(runIds.last, "Node0", since = Some(Time.fromMilliseconds(now + 15))))
    res should contain theSameElementsAs logs(runIds.last)("Node0").drop(16)
  }

  private def unsetResult(run: Run) = run.copy(state = run.state.copy(nodes = run.state.nodes.map(_.unsetResult)))

  private def randomId: RunId = RunId(UUID.randomUUID().toString)
}

private[storage] trait RunRepositorySpecWithMemoization extends RunRepositorySpec {
  it should "memoize artifacts" in {
    val repo = createRepository
    repo.save(foobarRunWithNodes)
    repo.save(fooRunWithNodes)
    refreshBeforeSearch()

    repo.get(CacheKey("MyFooCacheKey")) shouldBe Some(foobarResults("FooNode"))
    repo.get(CacheKey("MyBarCacheKey")) shouldBe Some(foobarResults("BarNode"))
    repo.get(CacheKey("YourFooCacheKey")) shouldBe Some(fooResults("FooNode"))
    repo.get(CacheKey("UnknownKey")) shouldBe None
  }
}