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
import fr.cnrs.liris.testing.UnitSpec

/**
 * Common unit tests for all [[TaskRepository]] implementations, ensuring they all have consistent behavior.
 */
private[storage] abstract class TaskRepositorySpec extends UnitSpec {
  private[this] val now = System.currentTimeMillis()
  private[this] val ScheduledTask = Task(
    id = randomId,
    runId = RunId("foobar"),
    nodeName = "foonode",
    payload = OpPayload("fooop", 1234, Map.empty, CacheKey("MyCacheKey")),
    createdAt = now,
    state = TaskState(TaskStatus.Scheduled, key = Some("fookey")))
  private[this] val RunningTask = Task(
    id = randomId,
    runId = RunId("foobar"),
    nodeName = "foonode",
    payload = OpPayload("fooop", 1234, Map.empty, CacheKey("MyCacheKey")),
    createdAt = now,
    state = TaskState(TaskStatus.Running, heartbeatAt = Some(now), key = Some("fookey")))
  private[this] val OtherRunningTask = Task(
    id = randomId,
    runId = RunId("barfoo"),
    nodeName = "foonode",
    payload = OpPayload("fooop", 1234, Map.empty, CacheKey("MyCacheKey")),
    createdAt = now,
    state = TaskState(TaskStatus.Running, heartbeatAt = Some(now), key = Some("fookey")))
  private[this] val ExpiredTask = Task(
    id = randomId,
    runId = RunId("bar"),
    nodeName = "foonode",
    payload = OpPayload("fooop", 1234, Map.empty, CacheKey("MyCacheKey")),
    createdAt = now,
    state = TaskState(TaskStatus.Running, heartbeatAt = Some(now - 30 * 1000), key = Some("fookey")))
  private[this] val OtherExpiredTask = Task(
    id = randomId,
    runId = RunId("barfoo"),
    nodeName = "foonode",
    payload = OpPayload("fooop", 1234, Map.empty, CacheKey("MyCacheKey")),
    createdAt = now,
    state = TaskState(TaskStatus.Running, heartbeatAt = None, key = Some("fookey")))

  protected def createRepository: MutableTaskRepository

  protected def refreshBeforeSearch(): Unit = {}

  it should "save and retrieve a task" in {
    val repo = createRepository
    repo.get(RunningTask.id) shouldBe None
    repo.save(RunningTask)
    refreshBeforeSearch()
    repo.get(RunningTask.id) shouldBe Some(RunningTask)

    val newTask = RunningTask.copy(nodeName = "barnode")
    repo.save(newTask)
    refreshBeforeSearch()
    repo.get(RunningTask.id) shouldBe Some(newTask)
  }

  it should "search for tasks" in {
    val repo = createRepository
    repo.save(ScheduledTask)
    repo.save(RunningTask)
    repo.save(OtherRunningTask)
    repo.save(ExpiredTask)
    repo.save(OtherExpiredTask)
    refreshBeforeSearch()

    var res = repo.find(TaskQuery())
    res should contain theSameElementsAs Set(ScheduledTask, RunningTask, OtherRunningTask, ExpiredTask, OtherExpiredTask)

    res = repo.find(TaskQuery(runs = Set(RunId("foobar"), RunId("barfoo"))))
    res should contain theSameElementsAs Set(ScheduledTask, RunningTask, OtherRunningTask, OtherExpiredTask)

    res = repo.find(TaskQuery(lostAt = Some(Time.fromMilliseconds(now - 20 * 1000))))
    res should contain theSameElementsAs Set(ExpiredTask, OtherExpiredTask)
  }

  it should "delete a task" in {
    val repo = createRepository
    repo.save(ScheduledTask)
    repo.save(RunningTask)
    refreshBeforeSearch()

    repo.remove(RunningTask.id)
    refreshBeforeSearch()
    repo.get(ScheduledTask.id) shouldBe Some(ScheduledTask)
    repo.get(RunningTask.id) shouldBe None
  }

  private def randomId: TaskId = TaskId(UUID.randomUUID().toString)
}