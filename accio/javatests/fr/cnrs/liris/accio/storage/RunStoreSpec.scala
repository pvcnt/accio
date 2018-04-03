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

package fr.cnrs.liris.accio.storage

import java.util.UUID

import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfterEach

import scala.collection.Map

/**
 * Common unit tests for all [[RunStore.Mutable]] implementations, ensuring they all have
 * a consistent behavior.
 */
private[storage] abstract class RunStoreSpec extends UnitSpec with BeforeAndAfterEach {
  protected val foobarRun = Run(
    id = "foobar",
    pkg = Package("my_workflow", Some("v1")),
    cluster = "default",
    owner = Some(User("me")),
    name = Some("foo bar workflow"),
    notes = Some("awesome workflow!"),
    tags = Set("foo", "bar"),
    seed = 1234,
    params = Map.empty,
    createdAt = System.currentTimeMillis(),
    state = RunStatus(status = TaskState.Scheduled, progress = 0))

  protected val fooRun = Run(
    id = "foo",
    pkg = Package("my_workflow", Some("v1")),
    cluster = "default",
    owner = Some(User("me")),
    name = Some("foo bar workflow"),
    tags = Set("foo"),
    seed = 54321,
    params = Map.empty,
    createdAt = System.currentTimeMillis() - 1000,
    state = RunStatus(status = TaskState.Running, progress = .5))

  private val runs = Seq(
    foobarRun,
    foobarRun.copy(
      id = randomId,
      createdAt = System.currentTimeMillis() + 10,
      state = foobarRun.state.copy(status = TaskState.Running),
      tags = Set("foo")),
    foobarRun.copy(
      id = randomId,
      createdAt = System.currentTimeMillis() + 40,
      state = foobarRun.state.copy(status = TaskState.Running),
      owner = Some(User("him")),
      tags = Set("foobar")),
    foobarRun.copy(
      id = randomId,
      createdAt = System.currentTimeMillis() + 50,
      pkg = Package("other_workflow", Some("v1"))),
    foobarRun.copy(
      id = randomId,
      createdAt = System.currentTimeMillis() + 60,
      parent = Some(foobarRun.id),
      tags = Set.empty))

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
    it should "save and retrieve runs" in {
      storage.write { stores =>
        stores.runs.get(foobarRun.id) shouldBe None
        stores.runs.get(fooRun.id) shouldBe None

        stores.runs.save(foobarRun)
        stores.runs.get(foobarRun.id) shouldBe Some(foobarRun)

        stores.runs.save(fooRun)
        stores.runs.get(fooRun.id) shouldBe Some(fooRun)
      }
    }

    it should "delete runs" in {
      storage.write { stores =>
        stores.runs.save(foobarRun)
        stores.runs.save(fooRun)

        stores.runs.delete(foobarRun.id)
        stores.runs.get(fooRun.id) shouldBe Some(fooRun)
        stores.runs.get(foobarRun.id) shouldBe None
      }
    }

    it should "search for runs by owner" in {
      storage.write { stores =>
        runs.foreach(stores.runs.save)
      }
      storage.read { stores =>
        var res = stores.runs.list(RunQuery(owner = Some("me")))
        res.totalCount shouldBe 3
        res.results should contain theSameElementsInOrderAs Seq(runs(3), runs(1), runs(0))

        res = stores.runs.list(RunQuery(owner = Some("me"), limit = Some(2)))
        res.totalCount shouldBe 3
        res.results should contain theSameElementsInOrderAs Seq(runs(3), runs(1))

        res = stores.runs.list(RunQuery(owner = Some("me"), limit = Some(2), offset = Some(2)))
        res.totalCount shouldBe 3
        res.results should contain theSameElementsInOrderAs Seq(runs(0))

        res = stores.runs.list(RunQuery(owner = Some("him")))
        res.totalCount shouldBe 1
        res.results should contain theSameElementsInOrderAs Seq(runs(2))
      }
    }

    it should "search for runs by workflow" in {
      storage.write { stores =>
        runs.foreach(stores.runs.save)
      }
      storage.read { stores =>
        val res = stores.runs.list(RunQuery(workflow = Some("other_workflow")))
        res.totalCount shouldBe 1
        res.results should contain theSameElementsInOrderAs Seq(runs(3))
      }
    }

    it should "search for runs by status" in {
      storage.write { stores =>
        runs.foreach(stores.runs.save)
      }
      storage.read { stores =>
        val res = stores.runs.list(RunQuery(status = Set(TaskState.Running)))
        res.totalCount shouldBe 2
        res.results should contain theSameElementsInOrderAs Seq(runs(2), runs(1))
      }
    }

    it should "search for runs by tags" in {
      storage.write { stores =>
        runs.foreach(stores.runs.save)
      }
      storage.read { stores =>
        var res = stores.runs.list(RunQuery(tags = Set("foo", "bar")))
        res.totalCount shouldBe 2
        res.results should contain theSameElementsInOrderAs Seq(runs(3), runs(0))

        res = stores.runs.list(RunQuery(tags = Set("foo")))
        res.totalCount shouldBe 3
        res.results should contain theSameElementsInOrderAs Seq(runs(3), runs(1), runs(0))

        res = stores.runs.list(RunQuery(tags = Set("foobar")))
        res.totalCount shouldBe 1
        res.results should contain theSameElementsInOrderAs Seq(runs(2))
      }
    }
  }

  private def randomId = UUID.randomUUID().toString
}