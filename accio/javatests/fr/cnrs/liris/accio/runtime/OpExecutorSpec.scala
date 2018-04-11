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

package fr.cnrs.liris.accio.runtime

import fr.cnrs.liris.accio.api.Values
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.testing.{CreateTmpDirectory, UnitSpec}
import fr.cnrs.liris.util.FileUtils
import fr.cnrs.liris.util.geo.Distance
import org.scalatest.BeforeAndAfterEach

/**
 * Unit tests for [[OpExecutor]].
 */
class OpExecutorSpec extends UnitSpec with CreateTmpDirectory with BeforeAndAfterEach {
  behavior of "OpExecutor"

  private[this] var executor: OpExecutor = _

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    executor = new OpExecutor(Set(
      OpMeta.apply[SimpleOp],
      OpMeta.apply[NoInputOp],
      OpMeta.apply[NoOutputOp],
      OpMeta.apply[ExceptionalOp],
      OpMeta.apply[UnstableOp],
      OpMeta.apply[InvalidUnstableOp]))
    executor.setWorkDir(tmpDir)
  }

  override protected def afterEach(): Unit = {
    FileUtils.safeDelete(tmpDir)
    executor = null
    super.afterEach()
  }

  it should "execute operators and return artifacts" in {
    var payload = OpPayload("Simple", 123, Seq(Artifact("str", Values.encodeString("foo"))), Resource(0, 0, 0))
    var res = executor.execute(payload)
    res.exitCode shouldBe 0
    res.artifacts should have size 2
    res.artifacts should contain(Artifact("str", Values.encodeString("foo+0")))
    res.artifacts should contain(Artifact("b", Values.encodeBoolean(false)))

    payload = OpPayload("Simple", 123, Seq(Artifact("str", Values.encodeString("bar")), Artifact("i", Values.encodeInteger(3))), Resource(0, 0, 0))
    res = executor.execute(payload)
    res.exitCode shouldBe 0
    res.artifacts should have size 2
    res.artifacts should contain(Artifact("str", Values.encodeString("bar+3")))
    res.artifacts should contain(Artifact("b", Values.encodeBoolean(true)))
  }

  it should "coerce input values when possible" in {
    val payload = OpPayload("Simple", 123, Seq(Artifact("str", Values.encodeInteger(2)), Artifact("i", Values.encodeString("3"))), Resource(0, 0, 0))
    val res = executor.execute(payload)
    res.exitCode shouldBe 0
    res.artifacts should have size 2
    res.artifacts should contain(Artifact("str", Values.encodeString("2+3")))
    res.artifacts should contain(Artifact("b", Values.encodeBoolean(true)))
  }

  it should "reject invalid inputs" in {
    val payload = OpPayload("Simple", 123, Seq(Artifact("str", Values.encodeString("foo")), Artifact("i", Values.encodeDistance(Distance.meters(2)))), Resource(0, 0, 0))
    val e = intercept[IllegalArgumentException] {
      executor.execute(payload)
    }
    e.getMessage shouldBe "Invalid input type for i: distance"
  }

  it should "execute operators with no input" in {
    val payload = OpPayload("NoInput", 123, Seq.empty, Resource(0, 0, 0))
    val res = executor.execute(payload)
    res.exitCode shouldBe 0
    res.artifacts should contain theSameElementsAs Seq(Artifact("s", Values.encodeString("foo")))
  }

  it should "execute operators with no output" in {
    val payload = OpPayload("NoOutput", 123, Seq(Artifact("s", Values.encodeString("foo"))), Resource(0, 0, 0))
    val res = executor.execute(payload)
    res.exitCode shouldBe 0
    res.artifacts should have size 0
  }

  it should "detect a missing input" in {
    val payload = OpPayload("Simple", 123, Seq.empty, Resource(0, 0, 0))
    val e = intercept[IllegalArgumentException] {
      executor.execute(payload)
    }
    e.getMessage shouldBe "Missing required input str"
  }

  it should "detect an unknown operator" in {
    val payload = OpPayload("Unknown", 123, Seq.empty, Resource(0, 0, 0))
    val e = intercept[IllegalArgumentException] {
      executor.execute(payload)
    }
    e.getMessage shouldBe "Unknown operator: Unknown"
  }

  it should "catch exceptions thrown by the operator" in {
    val payload = OpPayload("Exceptional", 123, Seq(Artifact("str", Values.encodeString("foo"))), Resource(0, 0, 0))
    val res = executor.execute(payload)
    res.exitCode shouldBe 1
    res.artifacts should have size 0
  }

  it should "give a seed to unstable operators" in {
    val payload = OpPayload("Unstable", 123, Seq.empty, Resource(0, 0, 0))
    val res = executor.execute(payload)
    res.exitCode shouldBe 0
    res.artifacts should contain theSameElementsAs Seq(Artifact("lng", Values.encodeLong(123)))
  }

  it should "not give a seed to non-unstable operators" in {
    val payload = OpPayload("InvalidUnstable", 123, Seq.empty, Resource(0, 0, 0))
    val res = executor.execute(payload)
    res.exitCode shouldBe 1
    res.artifacts should have size 0
  }
}