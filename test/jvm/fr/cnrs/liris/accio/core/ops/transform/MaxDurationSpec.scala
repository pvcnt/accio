package fr.cnrs.liris.accio.core.ops.transform

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[MaxDurationOp]].
 */
class MaxDurationSpec extends UnitSpec with WithTraceGenerator {
  "MaxDuration" should "shorten traces with a duration greater than threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    val t1 = transform(trace, Duration.standardSeconds(10 * 60 + 10))
    assertTraceIsShortened(trace, t1, 11)
  }

  it should "keep traces with a duration lower than threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    val t1 = transform(trace, Duration.standardMinutes(14))
    val t2 = transform(trace, Duration.standardMinutes(20))
    assertTraceIsShortened(trace, t1, 15)
    assertTraceIsShortened(trace, t2, 15)
  }

  private def transform(trace: Trace, duration: Duration) = {
    val res = MaxDurationOp(duration).transform(trace)
    res should have size 1
    res.head
  }

  private def assertTraceIsShortened(t: Trace, t1: Trace, s1: Int) = {
    t1.user shouldBe t.user
    t1.events shouldBe t.events.take(s1)
  }
}