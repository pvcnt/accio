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

package fr.cnrs.liris.accio.scheduler

import com.twitter.util.Await
import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfterEach

/**
 * Common unit tests for all [[Scheduler]] implementations, ensuring they all have a consistent
 * behavior.
 */
private[scheduler] trait SchedulerSpec extends BeforeAndAfterEach {
  this: UnitSpec =>

  private var scheduler: Scheduler = _

  protected def createScheduler: Scheduler

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    scheduler = createScheduler
  }

  override protected def afterEach(): Unit = {
    Await.result(scheduler.close())
    scheduler = null
    super.afterEach()
  }

  it should "launch a process" in {
    val process = Process("echo-process", "sleep 1 && echo foo")
    Await.result(scheduler.submit(process))
    Await.result(scheduler.isActive("echo-process")) shouldBe true
    Thread.sleep(2000)
    Await.result(scheduler.isCompleted("echo-process")) shouldBe true
  }

  it should "list logs for a process" in {
    val process = Process("echo-process", "echo foo && echo bar")
    Await.result(scheduler.submit(process))
    Thread.sleep(2000)

    Await.result(scheduler.isCompleted("echo-process")) shouldBe true
    Await.result(scheduler.listLogs("echo-process", "stdout")) should contain theSameElementsInOrderAs Seq("foo", "bar")
  }

  it should "kill a process" in {
    val process = Process("echo-process", "sleep 1000 && echo foo")
    Await.result(scheduler.submit(process))

    Await.result(scheduler.isActive("echo-process")) shouldBe true
    Await.result(scheduler.kill("echo-process"))
    Await.result(scheduler.isCompleted("echo-process")) shouldBe true
    Await.result(scheduler.listLogs("echo-process", "stdout")) should have size 0
  }

  it should "reject a duplicate process" in {
    val process = Process("echo-process", "sleep 1")
    Await.result(scheduler.submit(process))

    var res = Await.ready(scheduler.submit(process)).poll.get
    res.isThrow shouldBe true
    res.throwable.getMessage shouldBe "Process echo-process already exists"

    Thread.sleep(2000)
    res = Await.ready(scheduler.submit(process)).poll.get
    res.isThrow shouldBe true
    res.throwable.getMessage shouldBe "Process echo-process already exists"
  }
}