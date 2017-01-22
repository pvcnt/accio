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

import java.util.UUID

import com.twitter.util.Time
import fr.cnrs.liris.accio.core.domain.{WorkflowId, _}
import fr.cnrs.liris.testing.UnitSpec

import scala.collection.Map

/**
 * Common unit tests for all [[RunRepository]] implementations, ensuring they all have consistent behavior.
 */
private[storage] abstract class RunRepositorySpec extends UnitSpec {
  private[this] val foobarRun = Run(
    id = RunId("foobar"),
    pkg = Package(WorkflowId("my_workflow"), "v1"),
    owner = User("me"),
    name = Some("foo bar workflow"),
    notes = Some("awesome workflow!"),
    tags = Set("foo", "bar"),
    seed = 1234,
    params = Map.empty,
    createdAt = System.currentTimeMillis(),
    state = RunState(status = RunStatus.Scheduled, progress = 0))

  private[this] val fooRun = Run(
    id = RunId("foo"),
    pkg = Package(WorkflowId("my_workflow"), "v1"),
    owner = User("me"),
    name = Some("foo bar workflow"),
    tags = Set("foo"),
    seed = 54321,
    params = Map.empty,
    createdAt = System.currentTimeMillis() - 1000,
    state = RunState(status = RunStatus.Running, progress = .5))


  protected def createRepository: RunRepository

  protected def refreshBeforeSearch(): Unit = {}

  it should "save and retrieve runs" in {
    val repo = createRepository
    repo.contains(foobarRun.id) shouldBe false
    repo.get(foobarRun.id) shouldBe None
    repo.contains(fooRun.id) shouldBe false
    repo.get(fooRun.id) shouldBe None

    repo.save(foobarRun)
    refreshBeforeSearch()
    repo.contains(foobarRun.id) shouldBe true
    repo.get(foobarRun.id) shouldBe Some(foobarRun)

    repo.save(fooRun)
    refreshBeforeSearch()
    repo.get(fooRun.id) shouldBe Some(fooRun)
    repo.contains(fooRun.id) shouldBe true
  }

  it should "delete runs" in {
    val repo = createRepository
    repo.save(foobarRun)
    repo.contains(foobarRun.id) shouldBe true
    repo.remove(foobarRun.id)
    repo.contains(foobarRun.id) shouldBe false
  }

  it should "search for runs" in {
    val repo = createRepository
    val runs = Seq(
      foobarRun,
      foobarRun.copy(id = randomId, createdAt = System.currentTimeMillis() + 10),
      foobarRun.copy(id = randomId, createdAt = System.currentTimeMillis() + 40, owner = User("him")),
      foobarRun.copy(id = randomId, createdAt = System.currentTimeMillis() + 50, pkg = Package(WorkflowId("other_workflow"), "v1")))
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

  private[this] def randomId: RunId = RunId(UUID.randomUUID().toString)
}