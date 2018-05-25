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

package fr.cnrs.liris.accio.sdk

import fr.cnrs.liris.accio.domain.OpPayload
import fr.cnrs.liris.lumos.domain.{AttrValue, DataType, Value}
import fr.cnrs.liris.testing.{CreateTmpDirectory, UnitSpec}

/**
 * Unit tests for [[OpExecutor]].
 */
class OpExecutorSpec extends UnitSpec with CreateTmpDirectory {
  behavior of "OpExecutor"

  it should "execute operators and return artifacts" in {
    val executor = new OpExecutor(OpMetadata[SimpleOp], tmpDir)
    var payload = OpPayload("Simple", 123, Seq(AttrValue("str", Value.String("foo"))), Map.empty)
    var res = executor.execute(payload)
    res.successful shouldBe true
    res.artifacts should have size 2
    res.artifacts should contain(AttrValue("str", Value.String("foo+0")))
    res.artifacts should contain(AttrValue("b", Value.False))

    payload = OpPayload("Simple", 123, Seq(AttrValue("str", Value.String("bar")), AttrValue("i", Value.Int(3))), Map.empty)
    res = executor.execute(payload)
    res.successful shouldBe true
    res.artifacts should have size 2
    res.artifacts should contain(AttrValue("str", Value.String("bar+3")))
    res.artifacts should contain(AttrValue("b", Value.True))
  }

  it should "coerce input values when possible" in {
    val executor = new OpExecutor(OpMetadata[SimpleOp], tmpDir)
    val payload = OpPayload("Simple", 123, Seq(AttrValue("str", Value.Int(2)), AttrValue("i", Value.String("3"))), Map.empty)
    val res = executor.execute(payload)
    res.successful shouldBe true
    res.artifacts should have size 2
    res.artifacts should contain(AttrValue("str", Value.String("2+3")))
    res.artifacts should contain(AttrValue("b", Value.True))
  }

  it should "reject invalid inputs" in {
    val executor = new OpExecutor(OpMetadata[SimpleOp], tmpDir)
    val payload = OpPayload("Simple", 123, Seq(AttrValue("str", Value.String("foo")), AttrValue("i", Value.Double(2.4))), Map.empty)
    val res = executor.execute(payload)
    res.successful shouldBe false
    res.error.isDefined shouldBe true
    res.error.get.message shouldBe Some("Invalid input for i: 2.4")
  }

  it should "execute operators with no input" in {
    val executor = new OpExecutor(OpMetadata[NoInputOp], tmpDir)
    val payload = OpPayload("NoInput", 123, Seq.empty, Map.empty)
    val res = executor.execute(payload)
    res.successful shouldBe true
    res.artifacts should contain theSameElementsAs Seq(AttrValue("s", Value.String("foo")))
  }

  it should "execute operators with no output" in {
    val executor = new OpExecutor(OpMetadata[NoOutputOp], tmpDir)
    val payload = OpPayload("NoOutput", 123, Seq(AttrValue("s", Value.String("foo"))), Map.empty)
    val res = executor.execute(payload)
    res.successful shouldBe true
    res.artifacts should have size 0
  }

  it should "detect a missing input" in {
    val executor = new OpExecutor(OpMetadata[SimpleOp], tmpDir)
    val payload = OpPayload("Simple", 123, Seq.empty, Map.empty)
    val res = executor.execute(payload)
    res.successful shouldBe false
    res.error.isDefined shouldBe true
    res.error.get.message shouldBe Some("Missing required input str")
  }

  it should "catch exceptions thrown by the operator" in {
    val executor = new OpExecutor(OpMetadata[ExceptionalOp], tmpDir)
    val payload = OpPayload("Exceptional", 123, Seq(AttrValue("str", Value.String("foo"))), Map.empty)
    val res = executor.execute(payload)
    res.successful shouldBe false
    res.artifacts should have size 0
  }

  it should "give a seed to unstable operators" in {
    val executor = new OpExecutor(OpMetadata[UnstableOp], tmpDir)
    val payload = OpPayload("Unstable", 123, Seq.empty, Map.empty)
    val res = executor.execute(payload)
    res.successful shouldBe true
    res.artifacts should contain theSameElementsAs Seq(AttrValue("lng", Value.Long(123)))
  }

  it should "not give a seed to non-unstable operators" in {
    val executor = new OpExecutor(OpMetadata[InvalidUnstableOp], tmpDir)
    val payload = OpPayload("InvalidUnstable", 123, Seq.empty, Map.empty)
    val res = executor.execute(payload)
    res.successful shouldBe false
    res.artifacts should have size 0
  }
}