package fr.cnrs.liris.accio.ops

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

class UniformSamplingSpec extends UnitSpec with WithTraceGenerator {
  "UniformSampling" should "downsample traces" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    val runs = 10
    Seq.fill(runs)(transform(trace, 0.1).size).sum.toDouble shouldBe ((10d * runs) +- (2.5 * runs))
    Seq.fill(runs)(transform(trace, 0.5).size).sum.toDouble shouldBe ((50d * runs) +- (2.5 * runs))
    Seq.fill(runs)(transform(trace, 0.9).size).sum.toDouble shouldBe ((90d * runs) +- (2.5 * runs))
  }

  it should "handle null probability" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    val res = transform(trace, 0)
    res.user shouldBe trace.user
    res.events should have size 0
  }

  it should "handle certain probability" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    val res = transform(trace, 1)
    res.user shouldBe trace.user
    res.events shouldBe trace.events
  }

  private def transform(trace: Trace, probability: Double) = {
    val res = UniformSamplingOp(probability).transform(trace)
    res should have size 1
    res.head
  }
}
