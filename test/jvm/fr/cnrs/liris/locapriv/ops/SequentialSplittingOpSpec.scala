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

import fr.cnrs.liris.locapriv.model.Trace
import fr.cnrs.liris.locapriv.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[SequentialSplittingOp]].
 */
class SequentialSplittingOpSpec extends UnitSpec with WithTraceGenerator with OperatorSpec {
  behavior of "SequentialSplittingOp"

  it should "split a trace with an even number of events without losing any of them" in {
    val trace = randomTrace(Me, 150)
    val (out1, out2) = transform(Seq(trace), 50)
    out1 should have size 1
    out2 should have size 1
    assertTraceIsSplit(trace, out1.head, out2.head, 75)
  }

  it should "split a trace with a odd number of events without losing any of them" in {
    val trace = randomTrace(Me, 151)
    val (out1, out2) = transform(Seq(trace), 50)
    out1 should have size 1
    out2 should have size 1
    assertTraceIsSplit(trace, out1.head, out2.head, 76)
  }

  it should "split several traces" in {
    val trace1 = randomTrace(Me, 150)
    val trace2 = randomTrace(Him, 150)
    val (out1, out2) = transform(Seq(trace1, trace2), 50)
    out1 should have size 2
    out2 should have size 2
    assertTraceIsSplit(trace1, out1.find(_.user == Me).get, out2.find(_.user == Me).get, 75)
    assertTraceIsSplit(trace2, out1.find(_.user == Him).get, out2.find(_.user == Him).get, 75)
  }

  it should "split a dataset with an empty trace into two datasets with an empty trace" in {
    val trace = Trace.empty(Me)
    val (out1, out2) = transform(Seq(trace), 50)
    out1 should have size 1
    out2 should have size 1
    assertTraceIsSplit(trace, out1.head, out2.head, 0)
  }

  it should "split an empty dataset into two empty datasets" in {
    val (out1, out2) = transform(Seq.empty, .5)
    out1 should have size 0
    out2 should have size 0
  }

  it should "split a trace at 0%" in {
    val trace = randomTrace(Me, 150)
    val (out1, out2) = transform(Seq(trace), 0)
    out1 should have size 1
    out2 should have size 1
    assertTraceIsSplit(trace, out1.head, out2.head, 0)
  }

  it should "split a trace at 100%" in {
    val trace = randomTrace(Me, 150)
    val (out1, out2) = transform(Seq(trace), 100)
    out1 should have size 1
    out2 should have size 1
    assertTraceIsSplit(trace, out1.head, out2.head, 150)
  }

  private def transform(data: Seq[Trace], percent: Double): (Seq[Trace], Seq[Trace]) = {
    val ds = writeTraces(data: _*)
    val res1 = new SequentialSplittingOp().execute(SequentialSplittingIn(percentBegin = 0, percentEnd = percent, complement = false, data = ds), ctx)
    val res2 = new SequentialSplittingOp().execute(SequentialSplittingIn(percentBegin = 0, percentEnd = percent, complement = true, data = ds), ctx)
    (readTraces(res1.data), readTraces(res2.data))
  }

  private def assertTraceIsSplit(t: Trace, t1: Trace, t2: Trace, s1: Int): Unit = {
    t1.user shouldBe t.user
    t2.user shouldBe t.user
    t1.events should contain theSameElementsInOrderAs t.events.take(s1)
    t2.events should contain theSameElementsInOrderAs t.events.drop(s1)
  }
}