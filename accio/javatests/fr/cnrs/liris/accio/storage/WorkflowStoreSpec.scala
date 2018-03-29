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

import fr.cnrs.liris.accio.api.thrift.{AtomicType, DataType}
import fr.cnrs.liris.accio.api.{Values, thrift}
import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfterEach

/**
 * Common unit tests for all [[WorkflowStore.Mutable]] implementations, ensuring they all have
 * a consistent behavior.
 */
private[storage] abstract class WorkflowStoreSpec extends UnitSpec with BeforeAndAfterEach {
  private val workflow1 = thrift.Workflow(
    id = thrift.WorkflowId("workflow1"),
    version = Some("v1"),
    owner = Some(thrift.User("me")),
    createdAt = Some(System.currentTimeMillis()),
    graph = thrift.Graph(Set(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Value(Values.encodeInteger(42)))),
      thrift.Node(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> thrift.Input.Value(Values.encodeDouble(3.14)),
          "data" -> thrift.Input.Reference(thrift.Reference("FirstSimple", "data")))))),
    name = Some("my workflow"))

  private val workflow2 = thrift.Workflow(
    id = thrift.WorkflowId("workflow2"),
    version = Some("v1"),
    owner = Some(thrift.User("me")),
    params = Set(thrift.ArgDef("foo", DataType(AtomicType.Integer))),
    createdAt = Some(System.currentTimeMillis() + 10),
    graph = thrift.Graph(Set(
      thrift.Node(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> thrift.Input.Param("foo"))),
      thrift.Node(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> thrift.Input.Param("bar"),
          "data" -> thrift.Input.Reference(thrift.Reference("FirstSimple", "data")))))))

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
    it should "save and retrieve workflows" in {
      storage.write { stores =>
        stores.workflows.get(workflow1.id) shouldBe None
        stores.workflows.get(workflow2.id) shouldBe None

        stores.workflows.save(workflow1)
        stores.workflows.get(workflow1.id) shouldBe Some(workflow1)

        stores.workflows.save(workflow2)
        stores.workflows.get(workflow2.id) shouldBe Some(workflow2)
      }
    }

    it should "search for workflows" in {
      val workflows = Seq(
        workflow1,
        workflow1.copy(version = Some("v2")),
        workflow2,
        workflow2.copy(version = Some("v2")),
        workflow1.copy(id = thrift.WorkflowId("other_workflow"), createdAt = Some(System.currentTimeMillis() + 20)),
        workflow1.copy(id = thrift.WorkflowId("another_workflow"), createdAt = Some(System.currentTimeMillis() + 30), owner = Some(thrift.User("him"))))
      storage.write { stores =>
        workflows.foreach(stores.workflows.save)
      }
      storage.write { stores =>
        var res = stores.workflows.list(WorkflowQuery(owner = Some("me")))
        res.totalCount shouldBe 3
        res.results should contain theSameElementsInOrderAs Seq(workflows(4), workflows(3), workflows(1))

        res = stores.workflows.list(WorkflowQuery(owner = Some("me"), limit = Some(2)))
        res.totalCount shouldBe 3
        res.results should contain theSameElementsInOrderAs Seq(workflows(4), workflows(3))

        res = stores.workflows.list(WorkflowQuery(owner = Some("me"), limit = Some(2), offset = Some(2)))
        res.totalCount shouldBe 3
        res.results should contain theSameElementsInOrderAs Seq(workflows(1))
      }
    }

    it should "retrieve a workflow at a specific version" in {
      val workflows = Seq(
        workflow1,
        workflow1.copy(version = Some("v2")),
        workflow2)
      storage.write { stores =>
        workflows.foreach(stores.workflows.save)
      }
      storage.read { stores =>
        stores.workflows.get(workflow1.id, "v1") shouldBe Some(workflows(0))
        stores.workflows.get(workflow1.id, "v2") shouldBe Some(workflows(1))
        stores.workflows.get(workflow1.id, "v3") shouldBe None
        stores.workflows.get(workflow2.id, "v1") shouldBe Some(workflows(2))
        stores.workflows.get(workflow2.id, "v2") shouldBe None
      }
    }
  }
}