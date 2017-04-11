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

package fr.cnrs.liris.accio.core.scheduler

import com.twitter.util.{Duration, Time}
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[ClusterState]].
 */
class ClusterStateSpec extends UnitSpec {
  behavior of "ClusterState"

  it should "register workers" in {
    val state = createState()
    state.register(WorkerId("foo-worker"), "foo:9999", Resource(2, 4000, 40000))
    state.read(identity).map(_.id) should contain theSameElementsAs Set(WorkerId("foo-worker"))
    state(WorkerId("foo-worker")).id shouldBe WorkerId("foo-worker")
    state(WorkerId("foo-worker")).maxResources shouldBe Resource(2, 4000, 40000)
    state(WorkerId("foo-worker")).availableResources shouldBe Resource(2, 4000, 40000)
    state(WorkerId("foo-worker")).reservedResources shouldBe Resource(0, 0, 0)

    state.register(WorkerId("bar-worker"), "bar:9999", Resource(8, 8000, 100000))
    state.read(identity).map(_.id) should contain theSameElementsAs Set(WorkerId("foo-worker"), WorkerId("bar-worker"))
    state(WorkerId("bar-worker")).id shouldBe WorkerId("bar-worker")
    state(WorkerId("bar-worker")).maxResources shouldBe Resource(8, 8000, 100000)
    state(WorkerId("bar-worker")).availableResources shouldBe Resource(8, 8000, 100000)
    state(WorkerId("bar-worker")).reservedResources shouldBe Resource(0, 0, 0)
  }

  it should "assign and un-assign tasks" in {
    val state = createState()
    state.register(WorkerId("foo-worker"), "foo:9999", Resource(2, 4000, 40000))
    state.assign(WorkerId("foo-worker"), createTask(TaskId("foo-task"), Resource(1, 1000, 0)))
    state.ensure(WorkerId("foo-worker"), TaskId("foo-task"))
    state(WorkerId("foo-worker")).activeTasks.map(_.id) should contain theSameElementsAs Set(TaskId("foo-task"))
    state(WorkerId("foo-worker")).availableResources shouldBe Resource(1, 3000, 40000)
    state(WorkerId("foo-worker")).reservedResources shouldBe Resource(1, 1000, 0)

    state.assign(WorkerId("foo-worker"), createTask(TaskId("bar-task"), Resource(1, 2000, 10000)))
    state.ensure(WorkerId("foo-worker"), TaskId("bar-task"))
    state(WorkerId("foo-worker")).activeTasks.map(_.id) should contain theSameElementsAs Set(TaskId("foo-task"), TaskId("bar-task"))
    state(WorkerId("foo-worker")).availableResources shouldBe Resource(0, 1000, 30000)
    state(WorkerId("foo-worker")).reservedResources shouldBe Resource(2, 3000, 10000)

    // -> Running
    state.update(WorkerId("foo-worker"), TaskId("foo-task"), NodeStatus.Running)
    state(WorkerId("foo-worker")).activeTasks.find(_.id == TaskId("foo-task")).get.status shouldBe NodeStatus.Running
    state(WorkerId("foo-worker")).availableResources shouldBe Resource(0, 1000, 30000)
    state(WorkerId("foo-worker")).reservedResources shouldBe Resource(2, 3000, 10000)

    // -> Done
    state.update(WorkerId("foo-worker"), TaskId("foo-task"), NodeStatus.Success)
    state(WorkerId("foo-worker")).activeTasks.map(_.id) should contain theSameElementsAs Set(TaskId("bar-task"))
    state(WorkerId("foo-worker")).availableResources shouldBe Resource(1, 2000, 30000)
    state(WorkerId("foo-worker")).reservedResources shouldBe Resource(1, 2000, 10000)
    state(WorkerId("foo-worker")).completedTasks shouldBe 1
    state(WorkerId("foo-worker")).lostTasks shouldBe 0

    // -> Lost
    state.update(WorkerId("foo-worker"), TaskId("bar-task"), NodeStatus.Lost)
    state(WorkerId("foo-worker")).activeTasks should have size 0
    state(WorkerId("foo-worker")).availableResources shouldBe Resource(2, 4000, 40000)
    state(WorkerId("foo-worker")).reservedResources shouldBe Resource(0, 0, 0)
    state(WorkerId("foo-worker")).completedTasks shouldBe 1
    state(WorkerId("foo-worker")).lostTasks shouldBe 1
  }

  it should "not register an existing worker" in {
    val state = createState()
    state.register(WorkerId("foo-worker"), "foo:9999", Resource(2, 4000, 40000))
    an[InvalidWorkerException] shouldBe thrownBy {
      state.register(WorkerId("foo-worker"), "foo:9999", Resource(1, 14000, 10000))
    }
  }

  it should "not register an already-assigned task" in {
    val state = createState()
    state.register(WorkerId("foo-worker"), "foo:9999", Resource(2, 4000, 40000))
    state.register(WorkerId("bar-worker"), "bar:9999", Resource(2, 4000, 40000))
    val task = createTask(TaskId("foo-task"))
    state.assign(WorkerId("foo-worker"), task)
    an[InvalidTaskException] shouldBe thrownBy {
      state.assign(WorkerId("bar-worker"), task)
    }
  }

  it should "not register with an invalid worker" in {
    val state = createState()
    an[InvalidWorkerException] shouldBe thrownBy {
      state.assign(WorkerId("foo-worker"), createTask(TaskId("foo-task")))
    }
  }

  it should "un-register a worker" in {
    val state = createState()
    state.register(WorkerId("foo-worker"), "foo:9999", Resource(2, 4000, 40000))
    state.register(WorkerId("bar-worker"), "bar:9999", Resource(2, 4000, 40000))
    state.unregister(WorkerId("foo-worker"))

    state.read(identity).map(_.id) should contain theSameElementsAs Set(WorkerId("bar-worker"))
    an[InvalidWorkerException] shouldBe thrownBy {
      state(WorkerId("foo-worker"))
    }
  }

  it should "not un-register an unknown worker" in {
    val state = createState()
    an[InvalidWorkerException] shouldBe thrownBy {
      state.unregister(WorkerId("foo-worker"))
    }
  }

  it should "ensure a task is correctly registered" in {
    val state = createState()
    state.register(WorkerId("foo-worker"), "foo:9999", Resource(2, 4000, 40000))
    state.register(WorkerId("foo2-worker"), "foo:9999", Resource(2, 4000, 40000))
    state.assign(WorkerId("foo-worker"), createTask(TaskId("foo-task")))
    state.ensure(WorkerId("foo-worker"), TaskId("foo-task"))

    an[InvalidWorkerException] shouldBe thrownBy {
      // No such worker.
      state.ensure(WorkerId("bar-worker"), TaskId("foo-task"))
    }
    an[InvalidWorkerException] shouldBe thrownBy {
      // No such task.
      state.ensure(WorkerId("foo-worker"), TaskId("bar-task"))
    }
    an[InvalidWorkerException] shouldBe thrownBy {
      // Assigned to another worker.
      state.ensure(WorkerId("foo2-worker"), TaskId("foo-task"))
    }
  }

  it should "record heartbeats and find lost workers" in {
    val state = createState()
    val now = Time.now
    state.register(WorkerId("foo-worker"), "foo:9999", Resource(2, 4000, 40000))
    state.lostWorkers(now - Duration.fromSeconds(30)) should have size 0

    state.recordHeartbeat(WorkerId("foo-worker"), now + Duration.fromSeconds(1))
    state.lostWorkers(now) should have size 0
    state.lostWorkers(now + Duration.fromSeconds(2)).map(_.id) should contain theSameElementsAs Set(WorkerId("foo-worker"))

    state.register(WorkerId("bar-worker"), "bar:9999", Resource(2, 4000, 40000))
    state.recordHeartbeat(WorkerId("bar-worker"), now + Duration.fromSeconds(3))
    state.lostWorkers(now) should have size 0
    state.lostWorkers(now + Duration.fromSeconds(2)).map(_.id) should contain theSameElementsAs Set(WorkerId("foo-worker"))
    state.lostWorkers(now + Duration.fromSeconds(4)).map(_.id) should contain theSameElementsAs Set(WorkerId("foo-worker"), WorkerId("bar-worker"))

    state.recordHeartbeat(WorkerId("foo-worker"), now + Duration.fromSeconds(5))
    state.recordHeartbeat(WorkerId("bar-worker"), now + Duration.fromSeconds(5))
    state.lostWorkers(now) should have size 0
  }

  it should "not record heartbeats from unknown executor" in {
    val state = createState()
    an[InvalidWorkerException] shouldBe thrownBy {
      state.recordHeartbeat(WorkerId("foo-worker"))
    }
  }

  private def createState() = new ClusterState()

  private def createTask(id: TaskId, resource: Resource = Resource(1, 1000, 0)) = Task(
    id,
    RunId("foorun"),
    "somenode",
    OpPayload("someop", 1234, Map.empty, CacheKey("cachekey")),
    System.currentTimeMillis(),
    NodeStatus.Waiting,
    resource)
}
