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

package fr.cnrs.liris.accio.core.infra.statemgr

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.service.StateManager
import fr.cnrs.liris.testing.UnitSpec

/**
 * Common unit tests for all [[StateManager]] implementations, ensuring they all have consistent behavior.
 */
private[statemgr] abstract class StateMgrSpec extends UnitSpec {
  private[this] val ScheduledTask = Task(
    id = TaskId("id2"),
    runId = RunId("foobar"),
    nodeName = "foonode",
    payload = OpPayload("fooop", 1234, Map.empty),
    key = "fookey",
    scheduler = "dummy",
    createdAt = System.currentTimeMillis(),
    state = TaskState(TaskStatus.Scheduled))
  private[this] val RunningTask = Task(
    id = TaskId("id1"),
    runId = RunId("foobar"),
    nodeName = "foonode",
    payload = OpPayload("fooop", 1234, Map.empty),
    key = "fookey",
    scheduler = "dummy",
    createdAt = System.currentTimeMillis(),
    state = TaskState(TaskStatus.Running))

  protected def createStateMgr: StateManager

  it should "save and retrieve a task" in {
    val stateMgr = createStateMgr
    stateMgr.get(RunningTask.id) shouldBe None
    stateMgr.save(RunningTask)
    stateMgr.get(RunningTask.id) shouldBe Some(RunningTask)

    val newTask = RunningTask.copy(nodeName = "barnode")
    stateMgr.save(newTask)
    stateMgr.get(RunningTask.id) shouldBe Some(newTask)
  }

  it should "list all tasks" in {
    val stateMgr = createStateMgr
    stateMgr.save(ScheduledTask)
    stateMgr.save(RunningTask)
    stateMgr.tasks should contain theSameElementsAs Set(ScheduledTask, RunningTask)
  }

  it should "delete a task" in {
    val stateMgr = createStateMgr
    stateMgr.save(RunningTask)
    stateMgr.remove(RunningTask.id)
    stateMgr.get(RunningTask.id) shouldBe None
  }

  it should "create locks" in {
    val stateMgr = createStateMgr
    val lock = stateMgr.lock("my/lock")

    lock.lock()
    lock.unlock()

    doAsync(lock.lock()) shouldBe true // Other thread can lock now.
  }

  it should "prevent another thread to lock" in {
    val stateMgr = createStateMgr
    val lock1 = stateMgr.lock("my/lock")
    val lock2 = stateMgr.lock("my/lock")

    lock1.lock()
    doAsync(lock2.lock()) shouldBe false // Other thread cannot lock.
  }

  it should "create re-entrant locks" in {
    val stateMgr = createStateMgr
    val lock = stateMgr.lock("my/lock")

    doAsync {
      lock.lock()
      lock.lock() // Current thread can lock, it is reentrant.
    } shouldBe true
  }

  private def doAsync(f: => Unit) = {
    val pool = Executors.newSingleThreadExecutor
    val locked = new AtomicBoolean(false)
    val task = new Runnable {
      override def run(): Unit = {
        f
        locked.set(true)
      }
    }
    val future = pool.submit(task)

    Thread.sleep(2 * 1000)
    future.cancel(true)
    pool.shutdownNow()

    locked.get
  }
}