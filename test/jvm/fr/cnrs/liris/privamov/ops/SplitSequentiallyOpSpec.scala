package fr.cnrs.liris.privamov.ops

import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[SequentialSplittingOp]].
 */
class SplitSequentiallyOpSpec extends UnitSpec with WithTraceGenerator with WithSparkleEnv {
  behavior of "SplitSequentially"

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

  it should "split an empty trace into two empty traces" in {
    val trace = Trace.empty(Me)
    val (out1, out2) = transform(Seq(trace), 50)
    out1 should have size 1
    out2 should have size 1
    assertTraceIsSplit(trace, out1.head, out2.head, 0)
  }

  it should "split an empty iterator into two empty iterators" in {
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
    val ds = write(data: _*)
    val res1 = new SequentialSplittingOp(env).execute(SequentialSplittingIn(percentBegin = 0, percentEnd = percent, complement = false, data = ds), ctx)
    val res2 = new SequentialSplittingOp(env).execute(SequentialSplittingIn(percentBegin = 0, percentEnd = percent, complement = true, data = ds), ctx)
    (read(res1.data), read(res2.data))
  }

  private def assertTraceIsSplit(t: Trace, t1: Trace, t2: Trace, s1: Int): Unit = {
    t1.user shouldBe t.user
    t2.user shouldBe t.user
    t1.events shouldBe t.events.take(s1)
    t2.events shouldBe t.events.drop(s1)
  }
}