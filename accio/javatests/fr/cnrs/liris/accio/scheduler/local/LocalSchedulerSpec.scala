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

package fr.cnrs.liris.accio.scheduler.local

import com.google.common.eventbus.{EventBus, Subscribe}
import com.twitter.finagle.stats.NullStatsReceiver
import fr.cnrs.liris.accio.api.{TaskCompletedEvent, TaskStartedEvent}
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.testing.{CreateTmpDirectory, UnitSpec}
import org.scalatest.BeforeAndAfterEach

/**
 * Unit tests for [[LocalScheduler]].
 */
class LocalSchedulerSpec extends UnitSpec with CreateTmpDirectory with BeforeAndAfterEach {
  behavior of "LocalScheduler"

  private var scheduler: LocalScheduler = _
  private var eventBus: EventBus = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    eventBus = new EventBus()
    scheduler = new LocalScheduler(NullStatsReceiver, eventBus, Resource(0, 0, 0), "/dev/null", Seq.empty, true, tmpDir)
    scheduler.startUp()
  }

  override def afterEach(): Unit = {
    scheduler.shutDown()
    scheduler = null
    eventBus = null
    super.afterEach()
  }

  it should "launch tasks" in {
    var started = false
    var completed = false
    eventBus.register (new {
      @Subscribe
      def onTaskStart(e: TaskStartedEvent): Unit = {
        e.runId shouldBe "foo-run"
        e.nodeName shouldBe "node"
        started = true
      }

      @Subscribe
      def onTaskComplete(e: TaskCompletedEvent): Unit = {
        e.runId shouldBe "foo-run"
        e.nodeName shouldBe "node"
        e.cacheKey shouldBe "cache-key"
        completed = true
      }
    })
    val task = Task("foo-task", "foo-run", "node",  OpPayload("op", 0, Map.empty, "cache-key"), 0, TaskState.Waiting, Resource(0, 0, 0))
    scheduler.submit(task)
    Thread.sleep(2000)
    started shouldBe true
    completed shouldBe true
  }
}
