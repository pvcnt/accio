/*
 * Accio is a platform to launch computer science jobs.
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

package fr.cnrs.liris.lumos.storage

import com.twitter.util.{Await, Future}
import fr.cnrs.liris.lumos.domain._
import fr.cnrs.liris.testing.UnitSpec
import org.joda.time.Instant
import org.scalatest.BeforeAndAfterEach

/**
 * Common unit tests for all [[JobStore]] implementations, ensuring they all have a consistent
 * behavior.
 */
private[storage] abstract class JobStoreSpec extends UnitSpec with BeforeAndAfterEach {
  private[this] val now = Instant.now()
  private[this] val jobs = Seq(
    Job(
      name = "job1",
      createTime = now,
      owner = Some("me"),
      labels = Map("foo" -> "bar"),
      status = ExecStatus(state = ExecStatus.Running)),
    Job(
      name = "job2",
      createTime = now.plus(10),
      owner = Some("him"),
      labels = Map("bar" -> "foo", "foo" -> "bar")),
    Job(
      name = "job3",
      createTime = now.plus(20),
      owner = Some("him"),
      status = ExecStatus(state = ExecStatus.Running)),
    Job(
      name = "job4",
      createTime = now.plus(30),
      labels = Map("foo" -> "foo"),
      status = ExecStatus(state = ExecStatus.Successful)))

  protected def createStore: JobStore

  /**
   * Whether this test suite should be disabled. This allows to temporarily disable some tests,
   * e.g., if required dependencies are not available on the machine.
   */
  protected def disabled: Boolean = false

  /**
   * Instant of the [[JobStore]] being tested.
   */
  protected var store: JobStore = _

  override def beforeEach(): Unit = {
    if (!disabled) {
      store = createStore
      Await.result(store.startUp())
    }
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    super.afterEach()
    if (!disabled) {
      Await.result(store.close())
      store = null
    }
  }

  if (!disabled) {
    it should "create and retrieve jobs" in {
      Await.result(store.get("job1")) shouldBe None
      Await.result(store.get("job2")) shouldBe None
      jobs.foreach(job => Await.result(store.create(job)) shouldBe Status.Ok)
      Await.result(store.create(jobs.head)) shouldBe Status.AlreadyExists("job1")
      Await.result(store.get("job1")) shouldBe Some(jobs(0))
      Await.result(store.get("job2")) shouldBe Some(jobs(1))

      var res = Await.result(store.list())
      res.totalCount shouldBe 4
      res.jobs should contain theSameElementsInOrderAs jobs.reverse

      res = Await.result(store.list(limit = Some(2)))
      res.totalCount shouldBe 4
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(3), jobs(2))

      res = Await.result(store.list(offset = Some(2)))
      res.totalCount shouldBe 4
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(1), jobs(0))

      res = Await.result(store.list(limit = Some(2), offset = Some(1)))
      res.totalCount shouldBe 4
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(2), jobs(1))
    }

    it should "replace jobs" in {
      Await.result(store.replace(jobs.head)) shouldBe Status.NotFound("job1")
      Await.result(Future.join(jobs.map(store.create)))
      Await.result(store.replace(jobs.head.copy(labels = Map("foo" -> "otherbar")))) shouldBe Status.Ok
      Await.result(store.get("job1")) shouldBe Some(jobs.head.copy(labels = Map("foo" -> "otherbar")))
      Await.result(store.get("job2")) shouldBe Some(jobs(1))
      Await.result(store.get("job3")) shouldBe Some(jobs(2))
      Await.result(store.get("job4")) shouldBe Some(jobs(3))
    }

    it should "delete jobs" in {
      Await.result(store.delete("job4")) shouldBe Status.NotFound("job4")
      Await.result(Future.join(jobs.map(store.create)))
      Await.result(store.delete("job4")) shouldBe Status.Ok
      Await.result(store.get("job4")) shouldBe None
      Await.result(store.get("job1")) shouldBe Some(jobs(0))
      Await.result(store.get("job2")) shouldBe Some(jobs(1))
      Await.result(store.get("job3")) shouldBe Some(jobs(2))
    }

    it should "search for jobs by owner" in {
      Await.result(Future.join(jobs.map(store.create)))

      var res = Await.result(store.list(JobQuery(owner = Some("me"))))
      res.totalCount shouldBe 1
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(0))

      res = Await.result(store.list(JobQuery(owner = Some("him"))))
      res.totalCount shouldBe 2
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(2), jobs(1))

      res = Await.result(store.list(JobQuery(owner = Some("him")), limit = Some(1)))
      res.totalCount shouldBe 2
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(2))

      res = Await.result(store.list(JobQuery(owner = Some("him")), offset = Some(1)))
      res.totalCount shouldBe 2
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(1))
    }

    it should "search for jobs by labels" in {
      Await.result(Future.join(jobs.map(store.create)))

      var res = Await.result(store.list(JobQuery(labels = Some(LabelSelector.present("foo")))))
      res.totalCount shouldBe 3
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(3), jobs(1), jobs(0))

      res = Await.result(store.list(JobQuery(labels = Some(LabelSelector.present("foo"))), limit = Some(2)))
      res.totalCount shouldBe 3
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(3), jobs(1))

      res = Await.result(store.list(JobQuery(labels = Some(LabelSelector.present("foo"))), offset = Some(1)))
      res.totalCount shouldBe 3
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(1), jobs(0))

      res = Await.result(store.list(JobQuery(labels = Some(LabelSelector.in("foo", Set("bar"))))))
      res.totalCount shouldBe 2
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(1), jobs(0))

      res = Await.result(store.list(JobQuery(labels = Some(LabelSelector.in("foo", Set("foo", "bar"))))))
      res.totalCount shouldBe 3
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(3), jobs(1), jobs(0))

      res = Await.result(store.list(JobQuery(labels = Some(LabelSelector.notIn("foo", Set("bar"))))))
      res.totalCount shouldBe 2
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(3), jobs(2))

      res = Await.result(store.list(JobQuery(labels = Some(LabelSelector.absent("foo")))))
      res.totalCount shouldBe 1
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(2))

      res = Await.result(store.list(JobQuery(labels = Some(LabelSelector.in("foo", Set("bar")) + LabelSelector.present("bar")))))
      res.totalCount shouldBe 1
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(1))
    }

    it should "search for jobs by state" in {
      Await.result(Future.join(jobs.map(store.create)))

      var res = Await.result(store.list(JobQuery(state = Set(ExecStatus.Running))))
      res.totalCount shouldBe 2
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(2), jobs(0))

      res = Await.result(store.list(JobQuery(state = Set(ExecStatus.Running)), limit = Some(1)))
      res.totalCount shouldBe 2
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(2))

      res = Await.result(store.list(JobQuery(state = Set(ExecStatus.Running)), offset = Some(1)))
      res.totalCount shouldBe 2
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(0))

      res = Await.result(store.list(JobQuery(state = Set(ExecStatus.Running, ExecStatus.Successful))))
      res.totalCount shouldBe 3
      res.jobs should contain theSameElementsInOrderAs Seq(jobs(3), jobs(2), jobs(0))
    }
  }
}