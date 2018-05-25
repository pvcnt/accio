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

import com.twitter.io.Buf
import fr.cnrs.liris.accio.domain.{Channel, Step}
import fr.cnrs.liris.lumos.domain.{AttrValue, Value}
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[JsonWorkflowParser]].
 */
class JsonWorkflowParserSpec extends UnitSpec {
  behavior of "JsonWorkflowParser"

  private def parser = JsonWorkflowParser.default

  it should "parse a job definition" in {
    val workflow = parser.decode(Buf.Utf8(
      """{
        |"name": "my_workflow",
        |"labels": {"foo": "bar", "bar": "barbar"},
        |"params": [
        |  {"name": "foo", "value": true},
        |  {"name": "dbl", "value": 3.14},
        |  {"name": "dbl", "value": 42}
        |],
        |"seed": 1234567890123,
        |"steps": [
        |  {
        |    "op": "MyOp",
        |    "params": [
        |      {
        |        "name": "v",
        |        "source": {"constant": 14}
        |      },
        |      {
        |        "name": "data",
        |        "source": {
        |          "reference": {"step": "step2", "output": "out"}
        |        }
        |      }
        |    ]
        |  },
        |  {
        |    "name": "step2",
        |    "op": "OtherOp",
        |    "params": [
        |      {
        |        "name": "str",
        |        "source": {"constant": "bar"}
        |      },
        |      {
        |        "name": "p",
        |        "source": {"param": "foo"}
        |      }
        |    ]
        |  }
        |]
        |}""".stripMargin))
    workflow.name shouldBe "my_workflow"
    workflow.labels shouldBe Map("foo" -> "bar", "bar" -> "barbar")
    workflow.seed shouldBe 1234567890123L
    workflow.params shouldBe Seq(
      AttrValue("foo", Value.True),
      AttrValue("dbl", Value.Double(3.14)),
      AttrValue("dbl", Value.Int(42)))
    workflow.steps shouldBe Seq(
      Step(
        name = "",
        op = "MyOp",
        params = Seq(
          Channel("v", Channel.Constant(Value.Int(14))),
          Channel("data", Channel.Reference("step2", "out")))),
      Step(
        name = "step2",
        op = "OtherOp",
        params = Seq(
          Channel("str", Channel.Constant(Value.String("bar"))),
          Channel("p", Channel.Param("foo")))))
  }
}
