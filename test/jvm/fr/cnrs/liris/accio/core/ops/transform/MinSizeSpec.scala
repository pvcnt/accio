package fr.cnrs.liris.accio.core.ops.transform

import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[MinSizeOp]].
 */
class MinSizeSpec extends UnitSpec with WithTraceGenerator {
  "MinSize" should "keep traces with a length equal to threshold" in {
    val trace = randomTrace(Me, 15)
    MinSizeOp(15).transform(trace) shouldBe Seq(trace)
  }

  it should "keep traces with a length greater than threshold" in {
    val trace = randomTrace(Me, 15)
    MinSizeOp(10).transform(trace) shouldBe Seq(trace)
  }

  it should "reject traces with a length lower than threshold" in {
    val trace = randomTrace(Me, 15)
    MinSizeOp(16).transform(trace) shouldBe Seq.empty[Trace]
    MinSizeOp(20).transform(trace) shouldBe Seq.empty[Trace]
  }
}