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

package fr.cnrs.liris.accio.core.infra.statemgr

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

import fr.cnrs.liris.accio.core.service.StateManager
import fr.cnrs.liris.accio.testing.Tasks
import fr.cnrs.liris.testing.UnitSpec

/**
 * Common unit tests for all [[StateManager]] implementations, ensuring they all have consistent behavior.
 */
private[statemgr] abstract class StateMgrSpec extends UnitSpec {
  protected def createStateMgr: StateManager

  it should "save and retrieve a task" in {
    val stateMgr = createStateMgr
    stateMgr.get(Tasks.RunningTask.id) shouldBe None
    stateMgr.save(Tasks.RunningTask)
    stateMgr.get(Tasks.RunningTask.id) shouldBe Some(Tasks.RunningTask)

    val newTask = Tasks.RunningTask.copy(nodeName = "barnode")
    stateMgr.save(newTask)
    stateMgr.get(Tasks.RunningTask.id) shouldBe Some(newTask)
  }

  it should "list all tasks" in {
    val stateMgr = createStateMgr
    stateMgr.save(Tasks.ScheduledTask)
    stateMgr.save(Tasks.RunningTask)
    stateMgr.tasks shouldBe Set(Tasks.ScheduledTask, Tasks.RunningTask)
  }

  it should "delete a task" in {
    val stateMgr = createStateMgr
    stateMgr.save(Tasks.RunningTask)
    stateMgr.remove(Tasks.RunningTask.id)
    stateMgr.get(Tasks.RunningTask.id) shouldBe None
  }

  it should "create locks" in {
    val stateMgr = createStateMgr
    val lock1 = stateMgr.lock("my/lock")
    val lock2 = stateMgr.lock("my/lock")

    lock1.lock()
    //lock1.lock() // Current thread can lock, it is reentrant. => Not guaranteed by all implementations.
    doAsync(lock2.lock()) shouldBe false // Other thread cannot lock.
    lock1.unlock()

    doAsync {
      lock2.lock()
      lock2.unlock()
    } shouldBe true // Other thread can lock and unlock now.
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
    pool.submit(task)

    Thread.sleep(2 * 1000)
    pool.shutdownNow()

    locked.get
  }
}