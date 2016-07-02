package fr.cnrs.liris.accio.core.ops.transform

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

class UniformSamplingSpec extends UnitSpec with WithTraceGenerator {
  "UniformSampling" should "downsample traces" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    val runs = 10
    Seq.fill(runs)(transform(trace, 0.1).size).sum.toDouble shouldBe ((10d * runs) +- (2 * runs))
    Seq.fill(runs)(transform(trace, 0.5).size).sum.toDouble shouldBe ((50d * runs) +- (2 * runs))
    Seq.fill(runs)(transform(trace, 0.9).size).sum.toDouble shouldBe ((90d * runs) +- (2 * runs))
  }

  it should "handle null probability" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    transform(trace, 0) shouldBe Trace.empty(Me)
  }

  it should "handle certain probability" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    transform(trace, 1) shouldBe trace
  }

  private def transform(trace: Trace, probability: Double) = {
    val res = UniformSamplingOp(probability).transform(trace)
    res should have size 1
    res.head
  }
}
