package fr.cnrs.liris.accio.ops

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[EnforceDurationOp]].
 */
class EnforceDurationOpSpec extends UnitSpec with WithTraceGenerator {
  behavior of "EnforceDurationOp"

  it should "keep traces with a duration equal to threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    transformMinDuration(trace, Duration.standardMinutes(14)) should contain theSameElementsAs Seq(trace)
  }

  it should "keep traces with a duration greater than threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    transformMinDuration(trace, Duration.standardMinutes(10)) should contain theSameElementsAs Seq(trace)
  }

  it should "reject traces with a duration lower than threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    transformMinDuration(trace, Duration.standardMinutes(15)) should have size 0
    transformMinDuration(trace, Duration.standardMinutes(20)) should have size 0
  }

  it should "shorten traces with a duration greater than threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    val t1 = transformMaxDuration(trace, Duration.standardSeconds(10 * 60 + 10))
    assertTraceIsShortened(trace, t1, 11)
  }

  it should "keep traces with a duration lower than threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    val t1 = transformMaxDuration(trace, Duration.standardMinutes(14))
    val t2 = transformMaxDuration(trace, Duration.standardMinutes(20))
    assertTraceIsShortened(trace, t1, 15)
    assertTraceIsShortened(trace, t2, 15)
  }

  private def transformMinDuration(trace: Trace, duration: Duration) = {
    EnforceDurationOp(minDuration = Some(duration), maxDuration = None).transform(trace)
  }

  private def transformMaxDuration(trace: Trace, duration: Duration) = {
    val res = EnforceDurationOp(maxDuration = Some(duration), minDuration = None).transform(trace)
    res should have size 1
    res.head
  }

  private def assertTraceIsShortened(t: Trace, t1: Trace, s1: Int) = {
    t1.user shouldBe t.user
    t1.events shouldBe t.events.take(s1)
  }
}