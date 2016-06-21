package fr.cnrs.liris.accio.core.ops.transform

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.core.framework.BoundTransformer
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[MinDurationOp]].
 */
class MinDurationSpec extends UnitSpec with WithTraceGenerator {
  "MinDuration" should "keep traces with a duration equal to threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    transform(trace, Duration.standardMinutes(14)) shouldBe Seq(trace)
  }

  it should "keep traces with a duration greater than threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    transform(trace, Duration.standardMinutes(10)) shouldBe Seq(trace)
  }

  it should "reject traces with a duration lower than threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    transform(trace, Duration.standardMinutes(15)) shouldBe Seq.empty[Trace]
    transform(trace, Duration.standardMinutes(20)) shouldBe Seq.empty[Trace]
  }

  private def transform(trace: Trace, duration: Duration) = {
    val transformation = BoundTransformer(new MinDurationOp)(_.duration := duration)
    transformation.transform(trace)
  }
}