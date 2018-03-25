/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.agent.handler

import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.util.{Duration, Time}
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[WorkerState]].
 */
class WorkerStateSpec extends UnitSpec {
  behavior of "WorkerState"

  it should "register and assign a task" in {
    val state = createState()
    state.register(TaskId("foo-task"))
    state.assign(TaskId("foo-task"), ExecutorId("foo-exec"))
    state.ensure(TaskId("foo-task"), ExecutorId("foo-exec"))
  }

  it should "not register an existing task" in {
    val state = createState()
    state.register(TaskId("foo-task"))
    an[InvalidTaskException] shouldBe thrownBy {
      // Not-started task.
      state.register(TaskId("foo-task"))
    }
    state.assign(TaskId("foo-task"), ExecutorId("foo-exec"))
    an[InvalidTaskException] shouldBe thrownBy {
      // Started task.
      state.register(TaskId("foo-task"))
    }
  }

  it should "un-register a task" in {
    val state = createState()
    state.register(TaskId("foo-task"))
    state.unregister(TaskId("foo-task"))

    state.register(TaskId("bar-task"))
    state.assign(TaskId("bar-task"), ExecutorId("foo-exec"))
    state.unregister(TaskId("bar-task"))
    an[InvalidExecutorException] shouldBe thrownBy {
      state.ensure(TaskId("bar-task"), ExecutorId("foo-exec"))
    }
  }

  it should "not un-register an not registered task" in {
    val state = createState()
    an[InvalidTaskException] shouldBe thrownBy {
      state.unregister(TaskId("foo-task"))
    }
  }

  it should "un-assign a task" in {
    val state = createState()
    state.register(TaskId("foo-task"))
    state.assign(TaskId("foo-task"), ExecutorId("foo-exec"))

    state.unassign(ExecutorId("foo-exec"), TaskId("foo-task"))
    an[InvalidExecutorException] shouldBe thrownBy {
      state.ensure(TaskId("foo-task"), ExecutorId("foo-exec"))
    }
  }

  it should "not un-assign an not assigned task" in {
    val state = createState()
    an[InvalidExecutorException] shouldBe thrownBy {
      state.unassign(ExecutorId("foo-exec"), TaskId("foo-task"))
    }
    state.register(TaskId("foo-task"))
    an[InvalidExecutorException] shouldBe thrownBy {
      state.unassign(ExecutorId("foo-exec"), TaskId("foo-task"))
    }
  }

  it should "ensure a task is correctly registered" in {
    val state = createState()
    an[InvalidExecutorException] shouldBe thrownBy {
      state.ensure(TaskId("foo-task"), ExecutorId("foo-exec"))
    }
    state.register(TaskId("foo-task"))
    state.assign(TaskId("foo-task"), ExecutorId("foo-exec"))
    state.ensure(TaskId("foo-task"), ExecutorId("foo-exec"))
    an[InvalidExecutorException] shouldBe thrownBy {
      state.ensure(TaskId("foo-task"), ExecutorId("foo2exec"))
    }
    an[InvalidExecutorException] shouldBe thrownBy {
      state.ensure(TaskId("foo2-task"), ExecutorId("foo-exec"))
    }
  }

  it should "record heartbeats and find lost executors" in {
    val state = createState()
    val now = Time.now
    state.register(TaskId("foo-task"))
    state.register(TaskId("bar-task"))
    state.assign(TaskId("foo-task"), ExecutorId("foo-exec"))
    state.lostExecutors(now - Duration.fromSeconds(30)) should have size 0

    state.recordHeartbeat(ExecutorId("foo-exec"), now + Duration.fromSeconds(1))
    state.lostExecutors(now) should have size 0
    state.lostExecutors(now + Duration.fromSeconds(2)) should contain theSameElementsAs Set((ExecutorId("foo-exec"), TaskId("foo-task")))

    state.assign(TaskId("bar-task"), ExecutorId("bar-exec"))
    state.recordHeartbeat(ExecutorId("bar-exec"), now + Duration.fromSeconds(3))
    state.lostExecutors(now) should have size 0
    state.lostExecutors(now + Duration.fromSeconds(2)) should contain theSameElementsAs Set((ExecutorId("foo-exec"), TaskId("foo-task")))
    state.lostExecutors(now + Duration.fromSeconds(4)) should contain theSameElementsAs Set((ExecutorId("foo-exec"), TaskId("foo-task")), (ExecutorId("bar-exec"), TaskId("bar-task")))

    state.recordHeartbeat(ExecutorId("foo-exec"), now + Duration.fromSeconds(5))
    state.recordHeartbeat(ExecutorId("bar-exec"), now + Duration.fromSeconds(5))
    state.lostExecutors(now) should have size 0
  }

  it should "not record heartbeats from unknown executor" in {
    val state = createState()
    an[InvalidExecutorException] shouldBe thrownBy {
      state.recordHeartbeat(ExecutorId("foo-exec"))
    }
  }

  private def createState() = new WorkerState(NullStatsReceiver, "test-worker", Resource(0, 0, 0))
}