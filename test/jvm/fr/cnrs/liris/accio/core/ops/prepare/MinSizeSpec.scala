package fr.cnrs.liris.accio.core.ops.transform

import fr.cnrs.liris.accio.core.framework.BoundTransformer
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[MinSize]].
 */
class MinSizeSpec extends UnitSpec with WithTraceGenerator {
  "MinSize" should "keep traces with a length equal to threshold" in {
    val trace = randomTrace(Me, 15)
    transform(trace, 15) shouldBe Seq(trace)
  }

  it should "keep traces with a length greater than threshold" in {
    val trace = randomTrace(Me, 15)
    transform(trace, 10) shouldBe Seq(trace)
  }

  it should "reject traces with a length lower than threshold" in {
    val trace = randomTrace(Me, 15)
    transform(trace, 16) shouldBe Seq.empty[Trace]
    transform(trace, 20) shouldBe Seq.empty[Trace]
  }

  private def transform(trace: Trace, size: Int) = {
    val transformation = BoundTransformer(new MinSize)(_.size := size)
    transformation.transform(trace)
  }
}