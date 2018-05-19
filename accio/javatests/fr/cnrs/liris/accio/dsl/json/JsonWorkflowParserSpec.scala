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

package fr.cnrs.liris.accio.dsl.json

import fr.cnrs.liris.accio.api.Values
import fr.cnrs.liris.accio.api.thrift
import fr.cnrs.liris.accio.api.thrift.{Export, NamedChannel}
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[JsonWorkflowParser]].
 */
class JsonWorkflowParserSpec extends UnitSpec {
  private[this] val parser = new JsonWorkflowParser

  it should "parse a job definition" in {
    val job = parser.parse(
      """{
        |"name": "my_workflow",
        |"title": "named run",
        |"tags": ["my", "awesome", "run"],
        |"params": [
        |  {"name": "foo", "value": true},
        |  {"name": "dbl", "value": 3.14},
        |  {"name": "dbl", "value": 42}
        |],
        |"seed": 1234567890123,
        |"steps": [
        |  {
        |    "name": "step1",
        |    "op": "MyOp",
        |    "inputs": [
        |      {
        |        "name": "v",
        |        "channel": {"value": 14}
        |      },
        |      {
        |        "name": "data",
        |        "channel": {
        |          "reference": {"step": "step2", "output": "out"}
        |        }
        |      }
        |    ],
        |    "exports": [
        |      {"output": "data", "exportAs": "data1"}
        |    ]
        |  },
        |  {
        |    "name": "step2",
        |    "op": "OtherOp",
        |    "inputs": [
        |      {
        |        "name": "str",
        |        "channel": {"value": "bar"}
        |      },
        |      {
        |        "name": "p",
        |        "channel": {"param": "foo"}
        |      }
        |    ]
        |  }
        |]
        |}""".stripMargin)
    job.name shouldBe "my_workflow"
    job.title shouldBe Some("named run")
    job.tags shouldBe Set("my", "awesome", "run")
    job.seed shouldBe 1234567890123L
    job.params shouldBe Seq(
      thrift.NamedValue("foo", Values.encodeBoolean(true)),
      thrift.NamedValue("dbl", Values.encodeDouble(3.14)),
      thrift.NamedValue("dbl", Values.encodeInteger(42)))
    job.steps shouldBe Seq(
      thrift.Step(
        name = "step1",
        op = "MyOp",
        inputs = Seq(
          NamedChannel("v", thrift.Channel.Value(Values.encodeInteger(14))),
          NamedChannel("data", thrift.Channel.Reference(thrift.Reference("step2", "out")))),
        exports = Seq(Export(output = "data", exportAs = "data1"))),
      thrift.Step(
        name = "step2",
        op = "OtherOp",
        inputs = Seq(
          NamedChannel("str", thrift.Channel.Value(Values.encodeString("bar"))),
          NamedChannel("p", thrift.Channel.Param("foo")))))
  }
}
