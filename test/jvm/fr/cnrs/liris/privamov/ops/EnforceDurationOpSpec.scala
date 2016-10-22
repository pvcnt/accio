package fr.cnrs.liris.privamov.ops

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[EnforceDurationOp]].
 */
class EnforceDurationOpSpec extends UnitSpec with WithTraceGenerator with WithSparkleEnv {
  behavior of "EnforceDurationOp"

  it should "keep traces with a duration equal to threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    transformMinDuration(Seq(trace), Duration.standardMinutes(14)) should contain theSameElementsAs Seq(trace)
  }

  it should "keep traces with a duration greater than threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    transformMinDuration(Seq(trace), Duration.standardMinutes(10)) should contain theSameElementsAs Seq(trace)
  }

  it should "reject traces with a duration lower than threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    transformMinDuration(Seq(trace), Duration.standardMinutes(15)) should have size 0
    transformMinDuration(Seq(trace), Duration.standardMinutes(20)) should have size 0
  }

  it should "shorten traces with a duration greater than threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    val data = transformMaxDuration(Seq(trace), Duration.standardSeconds(10 * 60 + 10))
    data should have size 1
    assertTraceIsShortened(trace, data.head, 11)
  }

  it should "keep traces with a duration lower than threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    var data = transformMaxDuration(Seq(trace), Duration.standardMinutes(14))
    data should have size 1
    assertTraceIsShortened(trace, data.head, 15)
    data = transformMaxDuration(Seq(trace), Duration.standardMinutes(20))
    data should have size 1
    assertTraceIsShortened(trace, data.head, 15)
  }

  private def assertTraceIsShortened(t: Trace, t1: Trace, s1: Int) = {
    t1.user shouldBe t.user
    t1.events shouldBe t.events.take(s1)
  }

  private def transformMaxDuration(data: Seq[Trace], duration: Duration) = {
    val ds = write(data: _*)
    val res = new EnforceDurationOp(env).execute(EnforceDurationIn(maxDuration = Some(duration), minDuration = None, data = ds), ctx)
    read(res.data)
  }

  private def transformMinDuration(data: Seq[Trace], duration: Duration) = {
    val ds = write(data: _*)
    val res = new EnforceDurationOp(env).execute(EnforceDurationIn(minDuration = Some(duration), maxDuration = None, data = ds), ctx)
    read(res.data)
  }
}