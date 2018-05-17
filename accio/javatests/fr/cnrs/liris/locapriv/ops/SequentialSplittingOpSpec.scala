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

package fr.cnrs.liris.locapriv.ops

import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.locapriv.domain.Event
import fr.cnrs.liris.locapriv.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[SequentialSplittingOp]].
 */
class SequentialSplittingOpSpec extends UnitSpec with WithTraceGenerator with ScalaOperatorSpec {
  behavior of "SequentialSplittingOp"

  it should "handle an even number of events" in {
    val trace = randomTrace(Me, 150)
    val (out1, out2) = transform(trace, 50)
    out1 should contain theSameElementsInOrderAs trace.take(75)
    out2 should contain theSameElementsInOrderAs trace.drop(75)
  }

  it should "handle an odd number of events" in {
    val trace = randomTrace(Me, 151)
    val (out1, out2) = transform(trace, 50)
    out1 should contain theSameElementsInOrderAs trace.take(76)
    out2 should contain theSameElementsInOrderAs trace.drop(76)
  }

  it should "handle several traces" in {
    val trace1 = randomTrace(Me, 150)
    val trace2 = randomTrace(Him, 150)
    val (out1, out2) = transform(trace1 ++ trace2, 50)
    (out1.size + out2.size) shouldBe 300
    assertTraceIsSplit(trace1, out1.filter(_.id == Me), out2.filter(_.id == Me), 75)
    assertTraceIsSplit(trace2, out1.filter(_.id == Him), out2.filter(_.id == Him), 75)
  }

  it should "handle empty traces" in {
    val (out1, out2) = transform(Seq.empty, 50)
    out1 should have size 0
    out2 should have size 0
  }

  it should "split a trace at 0%" in {
    val trace = randomTrace(Me, 150)
    val (out1, out2) = transform(trace, 0)
    out1 should have size 0
    out2 should contain theSameElementsInOrderAs trace
  }

  it should "split a trace at 100%" in {
    val trace = randomTrace(Me, 150)
    val (out1, out2) = transform(trace, 100)
    out1 should contain theSameElementsInOrderAs trace
    out2 should have size 0
  }

  it should "reject invalid parameters" in {
    var e = intercept[IllegalArgumentException] {
      SequentialSplittingOp(percentBegin = -1, percentEnd = 90, data = RemoteFile("/dev/null"))
    }
    e.getMessage shouldBe "requirement failed: percentBegin must be between 0 and 100: -1"

    e = intercept[IllegalArgumentException] {
      SequentialSplittingOp(percentBegin = 10, percentEnd = 101, data = RemoteFile("/dev/null"))
    }
    e.getMessage shouldBe "requirement failed: percentEnd must be between 0 and 100: 101"

    e = intercept[IllegalArgumentException] {
      SequentialSplittingOp(percentBegin = 56, percentEnd = 55, data = RemoteFile("/dev/null"))
    }
    e.getMessage shouldBe "requirement failed: percentEnd must be greater than percentBegin: 55 < 56"
  }

  private def transform(data: Seq[Event], percent: Int): (Seq[Event], Seq[Event]) = {
    com.twitter.jvm.numProcs.let(1) {
      val ds = writeTraces(data: _*)
      val res1 = SequentialSplittingOp(percentBegin = 0, percentEnd = percent, complement = false, data = ds).execute(ctx)
      val res2 = SequentialSplittingOp(percentBegin = 0, percentEnd = percent, complement = true, data = ds).execute(ctx)
      (env.read[Event].csv(res1.data.uri).collect().toSeq,
        env.read[Event].csv(res2.data.uri).collect().toSeq)
    }
  }

  private def assertTraceIsSplit(t: Seq[Event], t1: Seq[Event], t2: Seq[Event], s1: Int): Unit = {
    t1 should contain theSameElementsInOrderAs t.take(s1)
    t2 should contain theSameElementsInOrderAs t.drop(s1)
  }
}