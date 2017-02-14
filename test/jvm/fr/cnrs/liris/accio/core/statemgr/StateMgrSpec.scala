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

package fr.cnrs.liris.accio.core.statemgr

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Callable, Executors}

import fr.cnrs.liris.testing.UnitSpec

/**
 * Common unit tests for all [[StateManager]] implementations, ensuring they all have consistent behavior.
 */
private[statemgr] abstract class StateMgrSpec extends UnitSpec {
  private[this] val pool = Executors.newCachedThreadPool

  /*private[this] val ScheduledTask = Task(
    id = TaskId("id2"),
    runId = RunId("foobar"),
    nodeName = "foonode",
    payload = OpPayload("fooop", 1234, Map.empty, CacheKey("MyCacheKey")),
    key = "fookey",
    createdAt = System.currentTimeMillis(),
    state = TaskState(TaskStatus.Scheduled))
  private[this] val RunningTask = Task(
    id = TaskId("id1"),
    runId = RunId("foobar"),
    nodeName = "foonode",
    payload = OpPayload("fooop", 1234, Map.empty, CacheKey("MyCacheKey")),
    key = "fookey",
    createdAt = System.currentTimeMillis(),
    state = TaskState(TaskStatus.Running))*/

  protected def createStateMgr: StateManager

  it should "save and retrieve a key" in {
    val stateMgr = createStateMgr
    stateMgr.get("foo") shouldBe None
    stateMgr.set("foo", "foo".getBytes)
    stateMgr.get("foo").map(_.deep) shouldBe Some("foo".getBytes.deep)

    stateMgr.get("bar") shouldBe None
    stateMgr.set("bar", "bar".getBytes)
    stateMgr.get("bar").map(_.deep) shouldBe Some("bar".getBytes.deep)

    stateMgr.get("foo/bar") shouldBe None
    stateMgr.set("foo/bar", "foobar".getBytes)
    stateMgr.get("foo/bar").map(_.deep) shouldBe Some("foobar".getBytes.deep)
  }

  it should "list keys" in {
    val stateMgr = createStateMgr
    stateMgr.set("foo", "foo".getBytes)
    stateMgr.set("bar", "bar".getBytes)
    stateMgr.set("foo/foo", "foo".getBytes)
    stateMgr.set("foo/bar", "foobar".getBytes)

    stateMgr.list("foo") should contain theSameElementsAs Set("foo/foo", "foo/bar")
  }

  it should "delete a key" in {
    val stateMgr = createStateMgr
    stateMgr.set("foo", "foo".getBytes)
    stateMgr.remove("foo")
    stateMgr.get("foo") shouldBe None
  }

  /*it should "save and retrieve a task" in {
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
  }*/

  it should "block-lock" in {
    val stateMgr = createStateMgr
    val lock = stateMgr.lock("my/lock")

    lock.lock()
    lock.unlock()

    tryAsync(lock.lock()) shouldBe true // Other thread can lock now.
  }

  it should "try-lock" in {
    val stateMgr = createStateMgr
    val lock = stateMgr.lock("my/lock")

    lock.lock()
    lock.unlock()

    doAsync(lock.tryLock()) shouldBe true // Other thread can lock now.
  }

  it should "prevent another thread to lock" in {
    val stateMgr = createStateMgr
    val lock = stateMgr.lock("my/lock")

    lock.lock()
    tryAsync(lock.lock()) shouldBe false // Other thread cannot lock.
    doAsync(lock.tryLock()) shouldBe false // Other thread cannot lock.
  }

  it should "create re-entrant locks" in {
    val stateMgr = createStateMgr
    val lock = stateMgr.lock("my/lock")

    tryAsync {
      lock.lock()
      lock.lock() // Current thread can lock, it is reentrant.
    } shouldBe true
  }

  private def tryAsync(f: => Unit): Boolean = {
    val completed = new AtomicBoolean(false)
    val task = new Runnable {
      override def run(): Unit = {
        f
        completed.set(true)
      }
    }
    val future = pool.submit(task)

    Thread.sleep(2 * 1000)
    future.cancel(true)

    completed.get
  }

  private def doAsync[T](f: => T): T = {
    val task = new Callable[T] {
      override def call(): T = {
        f
      }
    }
    val future = pool.submit(task)
    future.get
  }
}