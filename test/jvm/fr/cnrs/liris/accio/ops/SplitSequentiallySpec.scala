package fr.cnrs.liris.accio.ops

import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[SequentialSplittingOp]].
 */
class SplitSequentiallySpec extends UnitSpec with WithTraceGenerator {
  "SplitSequentially" should "split a trace with an even number of events without losing any of them" in {
    val trace = randomTrace(Me, 150)
    val (out1, out2) = split(percent = 50, trace)
    assertTraceIsSplit(trace, out1.head, out2.head, 75)
  }

  it should "split a trace with a odd number of events without losing any of them" in {
    val trace = randomTrace(Me, 151)
    val (out1, out2) = split(percent = 50, trace)
    assertTraceIsSplit(trace, out1.head, out2.head, 76)
  }

  it should "split several traces" in {
    val trace1 = randomTrace(Me, 150)
    val trace2 = randomTrace(Him, 150)
    val (out1, out2) = split(percent = 50, trace1, trace2)
    assertTraceIsSplit(trace1, out1.head, out2.head, 75)
    assertTraceIsSplit(trace2, out1.last, out2.last, 75)
  }

  it should "split an empty trace into two empty traces" in {
    val trace = Trace.empty(Me)
    val (out1, out2) = split(percent = 50, trace)
    assertTraceIsSplit(trace, out1.head, out2.head, 0)
  }

  it should "split an empty iterator into two empty iterators" in {
    split(.5)
  }

  it should "split a trace at 0%" in {
    val trace = randomTrace(Me, 150)
    val (out1, out2) = split(percent = 0, trace)
    assertTraceIsSplit(trace, out1.head, out2.head, 0)
  }

  it should "split a trace at 100%" in {
    val trace = randomTrace(Me, 150)
    val (out1, out2) = split(percent = 100, trace)
    assertTraceIsSplit(trace, out1.head, out2.head, 150)
  }

  private def split(percent: Double, traces: Trace*): (Seq[Trace], Seq[Trace]) = {
    val splitter1 = SequentialSplittingOp(percentBegin = 0, percentEnd = percent, complement = false)
    val splitter2 = SequentialSplittingOp(percentBegin = 0, percentEnd = percent, complement = true)
    val out1 = traces.flatMap(splitter1.transform)
    val out2 = traces.flatMap(splitter2.transform)
    out1 should have size traces.size
    out2 should have size traces.size
    (out1, out2)
  }

  private def assertTraceIsSplit(t: Trace, t1: Trace, t2: Trace, s1: Int): Unit = {
    t1.user shouldBe t.user
    t2.user shouldBe t.user
    t1.events shouldBe t.events.take(s1)
    t2.events shouldBe t.events.drop(s1)
  }
}