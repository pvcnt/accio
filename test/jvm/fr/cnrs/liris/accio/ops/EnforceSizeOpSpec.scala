package fr.cnrs.liris.accio.ops

import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[EnforceSizeOp]].
 */
class EnforceSizeOpSpec extends UnitSpec with WithTraceGenerator {
  behavior of "EnforceSizeOp"

  it should "keep traces with a length equal to threshold" in {
    val trace = randomTrace(Me, 15)
    EnforceSizeOp(minSize = Some(15), maxSize = None).transform(trace) shouldBe Seq(trace)
  }

  it should "keep traces with a length greater than threshold" in {
    val trace = randomTrace(Me, 15)
    EnforceSizeOp(minSize = Some(10), maxSize = None).transform(trace) shouldBe Seq(trace)
  }

  it should "reject traces with a length lower than threshold" in {
    val trace = randomTrace(Me, 15)
    EnforceSizeOp(minSize = Some(16), maxSize = None).transform(trace) should have size 0
    EnforceSizeOp(minSize = Some(20), maxSize = None).transform(trace) should have size 0
  }
}