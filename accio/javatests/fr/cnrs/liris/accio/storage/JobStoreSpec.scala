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

package fr.cnrs.liris.accio.storage

import com.twitter.util.{Await, Future}
import fr.cnrs.liris.accio.api.Values
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfterEach

import scala.collection.Map

/**
 * Common unit tests for all [[JobStore]] implementations, ensuring they all have a consistent
 * behavior.
 */
private[storage] abstract class JobStoreSpec extends UnitSpec with BeforeAndAfterEach {
  private[this] val now = System.currentTimeMillis()
  private[this] val jobs = Seq(
    Job(
      name = "exp1",
      createTime = now,
      author = Some("me"),
      title = Some("foo bar workflow"),
      tags = Set("foo", "bar"),
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Seq(NamedChannel("foo", Channel.Value(Values.encodeInteger(42))))),
        Step(
          op = "SecondSimple",
          name = "SecondSimple",
          inputs = Seq(
            NamedChannel("dbl", Channel.Value(Values.encodeDouble(3.14))),
            NamedChannel("data", Channel.Reference(Reference("FirstSimple", "data")))))),
      status = JobStatus(state = ExecState.Running)),
    Job(
      name = "exp2",
      createTime = now + 10,
      title = Some("other workflow"),
      author = Some("him"),
      parent = Some("exp1"),
      tags = Set("bar", "foo", "bar bar")),
    Job(
      name = "exp3",
      createTime = now + 20,
      author = Some("him"),
      parent = Some("exp1"),
      steps = Seq(),
      status = JobStatus(state = ExecState.Running)),
    Job(
      name = "exp4",
      createTime = now + 30,
      title = Some("bar workflow"),
      tags = Set("foo"),
      steps = Seq(),
      status = JobStatus(state = ExecState.Successful)))

  protected def createStorage: Storage

  protected def disabled: Boolean = false

  protected var storage: Storage = _

  override def beforeEach(): Unit = {
    if (!disabled) {
      storage = createStorage
      storage.startUp()
    }
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    super.afterEach()
    if (!disabled) {
      storage.shutDown()
      storage = null
    }
  }

  if (!disabled) {
    it should "create and retrieve jobs" in {
      Await.result(storage.jobs.get("exp1")) shouldBe None
      Await.result(storage.jobs.get("exp2")) shouldBe None
      jobs.foreach(job => Await.result(storage.jobs.create(job)) shouldBe true)
      Await.result(storage.jobs.create(jobs.head)) shouldBe false
      Await.result(storage.jobs.get("exp1")) shouldBe Some(jobs(0))
      Await.result(storage.jobs.get("exp2")) shouldBe Some(jobs(1))

      var res = Await.result(storage.jobs.list())
      res.totalCount shouldBe 4
      res.results should contain theSameElementsInOrderAs jobs.reverse

      res = Await.result(storage.jobs.list(limit = Some(2)))
      res.totalCount shouldBe 4
      res.results should contain theSameElementsInOrderAs Seq(jobs(3), jobs(2))

      res = Await.result(storage.jobs.list(offset = Some(2)))
      res.totalCount shouldBe 4
      res.results should contain theSameElementsInOrderAs Seq(jobs(1), jobs(0))

      res = Await.result(storage.jobs.list(limit = Some(2), offset = Some(1)))
      res.totalCount shouldBe 4
      res.results should contain theSameElementsInOrderAs Seq(jobs(2), jobs(1))
    }

    it should "replace jobs" in {
      Await.result(storage.jobs.replace(jobs.head)) shouldBe false
      Await.result(Future.join(jobs.map(storage.jobs.create)))
      Await.result(storage.jobs.replace(jobs.head.copy(tags = Set("edited")))) shouldBe true
      Await.result(storage.jobs.get("exp1")) shouldBe Some(jobs.head.copy(tags = Set("edited")))
      Await.result(storage.jobs.get("exp2")) shouldBe Some(jobs(1))
      Await.result(storage.jobs.get("exp3")) shouldBe Some(jobs(2))
      Await.result(storage.jobs.get("exp4")) shouldBe Some(jobs(3))
    }

    it should "delete jobs" in {
      Await.result(storage.jobs.delete("exp4")) shouldBe false
      Await.result(Future.join(jobs.map(storage.jobs.create)))
      Await.result(storage.jobs.delete("exp4")) shouldBe true
      Await.result(storage.jobs.get("exp4")) shouldBe None
      Await.result(storage.jobs.get("exp1")) shouldBe Some(jobs(0))
      Await.result(storage.jobs.get("exp2")) shouldBe Some(jobs(1))
      Await.result(storage.jobs.get("exp3")) shouldBe Some(jobs(2))
    }

    it should "search for jobs by author" in {
      Await.result(Future.join(jobs.map(storage.jobs.create)))

      var res = Await.result(storage.jobs.list(JobStore.Query(author = Some("me"))))
      res.totalCount shouldBe 1
      res.results should contain theSameElementsInOrderAs Seq(jobs(0))

      res = Await.result(storage.jobs.list(JobStore.Query(author = Some("him"))))
      res.totalCount shouldBe 2
      res.results should contain theSameElementsInOrderAs Seq(jobs(2), jobs(1))

      res = Await.result(storage.jobs.list(JobStore.Query(author = Some("him")), limit = Some(1)))
      res.totalCount shouldBe 2
      res.results should contain theSameElementsInOrderAs Seq(jobs(2))

      res = Await.result(storage.jobs.list(JobStore.Query(author = Some("him")), offset = Some(1)))
      res.totalCount shouldBe 2
      res.results should contain theSameElementsInOrderAs Seq(jobs(1))
    }

    it should "search for jobs by title" in {
      Await.result(Future.join(jobs.map(storage.jobs.create)))

      var res = Await.result(storage.jobs.list(JobStore.Query(title = Some("workflow"))))
      res.totalCount shouldBe 3
      res.results should contain theSameElementsInOrderAs Seq(jobs(3), jobs(1), jobs(0))

      res = Await.result(storage.jobs.list(JobStore.Query(title = Some("workflow")), limit = Some(2)))
      res.totalCount shouldBe 3
      res.results should contain theSameElementsInOrderAs Seq(jobs(3), jobs(1))

      res = Await.result(storage.jobs.list(JobStore.Query(title = Some("workflow")), offset = Some(1)))
      res.totalCount shouldBe 3
      res.results should contain theSameElementsInOrderAs Seq(jobs(1), jobs(0))

      res = Await.result(storage.jobs.list(JobStore.Query(title = Some("bar"))))
      res.totalCount shouldBe 2
      res.results should contain theSameElementsInOrderAs Seq(jobs(3), jobs(0))

      res = Await.result(storage.jobs.list(JobStore.Query(title = Some("foo"))))
      res.totalCount shouldBe 1
      res.results should contain theSameElementsInOrderAs Seq(jobs(0))
    }

    it should "search for jobs by tags" in {
      Await.result(Future.join(jobs.map(storage.jobs.create)))

      var res = Await.result(storage.jobs.list(JobStore.Query(tags = Set("foo"))))
      res.totalCount shouldBe 3
      res.results should contain theSameElementsInOrderAs Seq(jobs(3), jobs(1), jobs(0))

      res = Await.result(storage.jobs.list(JobStore.Query(tags = Set("foo")), limit = Some(2)))
      res.totalCount shouldBe 3
      res.results should contain theSameElementsInOrderAs Seq(jobs(3), jobs(1))

      res = Await.result(storage.jobs.list(JobStore.Query(tags = Set("foo")), offset = Some(1)))
      res.totalCount shouldBe 3
      res.results should contain theSameElementsInOrderAs Seq(jobs(1), jobs(0))

      res = Await.result(storage.jobs.list(JobStore.Query(tags = Set("bar"))))
      res.totalCount shouldBe 2
      res.results should contain theSameElementsInOrderAs Seq(jobs(1), jobs(0))

      res = Await.result(storage.jobs.list(JobStore.Query(tags = Set("foo", "bar"))))
      res.totalCount shouldBe 2
      res.results should contain theSameElementsInOrderAs Seq(jobs(1), jobs(0))
    }

    it should "search for jobs by state" in {
      Await.result(Future.join(jobs.map(storage.jobs.create)))

      var res = Await.result(storage.jobs.list(JobStore.Query(state = Some(Set(ExecState.Running)))))
      res.totalCount shouldBe 2
      res.results should contain theSameElementsInOrderAs Seq(jobs(2), jobs(0))

      res = Await.result(storage.jobs.list(JobStore.Query(state = Some(Set(ExecState.Running))), limit = Some(1)))
      res.totalCount shouldBe 2
      res.results should contain theSameElementsInOrderAs Seq(jobs(2))

      res = Await.result(storage.jobs.list(JobStore.Query(state = Some(Set(ExecState.Running))), offset = Some(1)))
      res.totalCount shouldBe 2
      res.results should contain theSameElementsInOrderAs Seq(jobs(0))

      res = Await.result(storage.jobs.list(JobStore.Query(state = Some(Set(ExecState.Running, ExecState.Successful)))))
      res.totalCount shouldBe 3
      res.results should contain theSameElementsInOrderAs Seq(jobs(3), jobs(2), jobs(0))
    }

    it should "search for jobs by parent" in {
      Await.result(Future.join(jobs.map(storage.jobs.create)))

      var res = Await.result(storage.jobs.list(JobStore.Query(parent = Some("exp1"))))
      res.totalCount shouldBe 2
      res.results should contain theSameElementsInOrderAs Seq(jobs(2), jobs(1))

      res = Await.result(storage.jobs.list(JobStore.Query(parent = Some("exp1")), limit = Some(1)))
      res.totalCount shouldBe 2
      res.results should contain theSameElementsInOrderAs Seq(jobs(2))

      res = Await.result(storage.jobs.list(JobStore.Query(parent = Some("exp1")), offset = Some(1)))
      res.totalCount shouldBe 2
      res.results should contain theSameElementsInOrderAs Seq(jobs(1))

      res = Await.result(storage.jobs.list(JobStore.Query(parent = Some(""))))
      res.totalCount shouldBe 2
      res.results should contain theSameElementsInOrderAs Seq(jobs(3), jobs(0))
    }
  }
}