package fr.cnrs.liris.privamov.ops

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

class UniformSamplingOpSpec extends UnitSpec with WithTraceGenerator with WithSparkleEnv {
  behavior of "UniformSamplingOp"

  it should "downsample traces" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    // Seed is fixed, no need for multiple runs.
    transform(Seq(trace), 0.1).map(_.size).sum.toDouble shouldBe (10d +- 3)
    transform(Seq(trace), 0.5).map(_.size).sum.toDouble shouldBe (50d +- 3)
    transform(Seq(trace), 0.9).map(_.size).sum.toDouble shouldBe (90d +- 3)
  }

  it should "handle null probability" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    val res = transform(Seq(trace), 0)
    res should have size 1
    res.head.user shouldBe trace.user
    res.head.events should have size 0
  }

  it should "handle certain probability" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    val res = transform(Seq(trace), 1)
    res should have size 1
    res.head.user shouldBe trace.user
    res.head.events shouldBe trace.events
  }

  private def transform(data: Seq[Trace], probability: Double) = {
    val ds = write(data: _*)
    val res = new UniformSamplingOp(env).execute(UniformSamplingIn(probability = probability, data = ds), ctx)
    read(res.data)
  }
}
