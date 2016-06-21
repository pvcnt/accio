package fr.cnrs.liris.accio.core.ops.transform

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.core.framework.BoundTransformer
import fr.cnrs.liris.accio.core.model.{Record, Trace}
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

class TemporalSamplingSpec extends UnitSpec with WithTraceGenerator {
  "TemporalSampling" should "downsample traces" in {
    val trace = Trace(Seq(
      Record(Me, Here, Now),
      Record(Me, Here, Now + 10.seconds),
      Record(Me, Here, Now + 19.seconds),
      Record(Me, Here, Now + 25.seconds),
      Record(Me, Here, Now + 34.seconds),
      Record(Me, Here, Now + 44.seconds)))
    transform(trace, 10.seconds) shouldBe Trace(Me, Seq(
      Record(Me, Here, Now),
      Record(Me, Here, Now + 10.seconds),
      Record(Me, Here, Now + 25.seconds),
      Record(Me, Here, Now + 44.seconds)))
  }

  it should "handle empty traces" in {
    val trace = Trace.empty(Me)
    transform(trace, 10.seconds) shouldBe trace
  }

  it should "handle singleton traces" in {
    val trace = Trace(Seq(Record(Me, Here, Now)))
    transform(trace, 10.seconds) shouldBe trace
  }

  private def transform(trace: Trace, duration: Duration) = {
    val transformation = BoundTransformer(new TemporalSamplingOp)(_.duration := duration)
    val res = transformation.transform(trace)
    res should have size 1
    res.head
  }
}