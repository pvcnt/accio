package fr.cnrs.liris.accio.ops

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[MinDurationOp]].
 */
class MinDurationSpec extends UnitSpec with WithTraceGenerator {
  "MinDuration" should "keep traces with a duration equal to threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    MinDurationOp(Duration.standardMinutes(14)).transform(trace) shouldBe Seq(trace)
  }

  it should "keep traces with a duration greater than threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    MinDurationOp(Duration.standardMinutes(10)).transform(trace) shouldBe Seq(trace)
  }

  it should "reject traces with a duration lower than threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    MinDurationOp(Duration.standardMinutes(15)).transform(trace) shouldBe Seq.empty[Trace]
    MinDurationOp(Duration.standardMinutes(20)).transform(trace) shouldBe Seq.empty[Trace]
  }
}