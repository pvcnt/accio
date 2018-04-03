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

import fr.cnrs.liris.accio.api.Values
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[ExperimentParser]].
 */
class ExperimentParserSpec extends UnitSpec {
  private[this] val parser = new ExperimentParser

  it should "parse a minimal experiment definition" in {
    val spec = parser.parse("""{"workflow": "my_workflow"}""")
    spec.pkg.workflowId shouldBe "my_workflow"
    spec.pkg.workflowVersion shouldBe None
    spec.owner shouldBe None
    spec.name shouldBe None
    spec.notes shouldBe None
    spec.tags shouldBe Set.empty
    spec.seed shouldBe None
    spec.params shouldBe Map.empty
    spec.repeat shouldBe None
    spec.clonedFrom shouldBe None
  }

  it should "parse a more complete experiment definition" in {
    val spec = parser.parse(
      """{"workflow": "my_workflow",
        |"name": "named run",
        |"notes": "All my notes",
        |"tags": ["my", "awesome", "run"],
        |"params": {"foo": {"values":["foo","bar"]}, "dbl": {"values": [3.14]}},
        |"seed": 1234567890123,
        |"repeat": 15}""".stripMargin)
    spec.pkg.workflowId shouldBe "my_workflow"
    spec.pkg.workflowVersion shouldBe None
    spec.owner shouldBe None // There is never an owner from definition.
    spec.name shouldBe Some("named run")
    spec.notes shouldBe Some("All my notes")
    spec.tags shouldBe Set("my", "awesome", "run")
    spec.seed shouldBe Some(1234567890123L)
    spec.params shouldBe Map(
      "foo" -> Seq(Values.encodeString("foo"), Values.encodeString("bar")),
      "dbl" -> Seq(Values.encodeDouble(3.14)))
    spec.repeat shouldBe Some(15)
    spec.clonedFrom shouldBe None // Cloned from is not supported ATM.
  }
}
