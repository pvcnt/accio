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

package fr.cnrs.liris.accio.dsl

import fr.cnrs.liris.accio.api.thrift.{InvalidSpecException, InvalidSpecMessage}
import fr.cnrs.liris.accio.api.{Values, thrift}
import fr.cnrs.liris.accio.service.RunFactory
import fr.cnrs.liris.accio.storage.memory.MemoryStorage
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[RunParser]].
 */
class RunParserSpec extends UnitSpec {
  private[this] val myWorkflow = thrift.Workflow(
    id = thrift.WorkflowId("my_workflow"),
    version = Some("v1"),
    owner = Some(thrift.User("me")),
    isActive = true,
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

  private[this] val parser = {
    val mapper = new ObjectMapperFactory().create()
    val storage = new MemoryStorage
    storage.workflows.save(myWorkflow)
    new RunParser(mapper, storage, new RunFactory(storage))
  }

  it should "parse a minimal run definition" in {
    val spec = parser.parse("""{"workflow": "my_workflow"}""", Map.empty)
    spec.pkg.workflowId shouldBe myWorkflow.id
    spec.pkg.workflowVersion shouldBe "v1"
    spec.owner shouldBe None
    spec.name shouldBe None
    spec.notes shouldBe None
    spec.tags shouldBe Set.empty
    spec.seed shouldBe None
    spec.params shouldBe Map.empty
    spec.repeat shouldBe None
    spec.clonedFrom shouldBe None
  }

  it should "parse a more complete run definition" in {
    val spec = parser.parse(
      """{"workflow": "my_workflow",
        |"name": "named run",
        |"notes": "All my notes",
        |"tags": ["my", "awesome", "run"],
        |"seed": 1234567890123,
        |"repeat": 15}""".stripMargin,
      Map.empty)
    spec.pkg.workflowId shouldBe myWorkflow.id
    spec.pkg.workflowVersion shouldBe "v1"
    spec.owner shouldBe None // There is never an owner from definition.
    spec.name shouldBe Some("named run")
    spec.notes shouldBe Some("All my notes")
    spec.tags shouldBe Set("my", "awesome", "run")
    spec.seed shouldBe Some(1234567890123L)
    spec.params shouldBe Map.empty
    spec.repeat shouldBe Some(15)
    spec.clonedFrom shouldBe None // Cloned from is not supported ATM.
  }

  it should "detect an invalid workflow" in {
    assertErrors(
      """{"workflow": "unknown_workflow"}""",
      InvalidSpecMessage("Workflow not found: unknown_workflow"))

    assertErrors(
      """{"workflow": "invalid:workflow:identifier"}""",
      InvalidSpecMessage("Invalid workflow specification: invalid:workflow:identifier"))
  }

  private def assertErrors(content: String, errors: InvalidSpecMessage*): Unit = {
    val expected = intercept[InvalidSpecException] {
      parser.parse(content, Map.empty)
    }
    expected.errors should contain theSameElementsAs errors
  }
}
