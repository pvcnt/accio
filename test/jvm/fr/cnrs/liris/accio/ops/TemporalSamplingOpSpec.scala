package fr.cnrs.liris.accio.ops

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.privamov.model.{Event, Trace}
import fr.cnrs.liris.privamov.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

class TemporalSamplingOpSpec extends UnitSpec with WithTraceGenerator with WithSparkleEnv {
  behavior of "TemporalSampling"

  it should "downsample traces" in {
    val trace = Trace(Seq(
      Event(Me, Here, Now),
      Event(Me, Here, Now + 10.seconds),
      Event(Me, Here, Now + 19.seconds),
      Event(Me, Here, Now + 25.seconds),
      Event(Me, Here, Now + 34.seconds),
      Event(Me, Here, Now + 44.seconds)))
    val res = transform(Seq(trace), 10.seconds)
    res should have size 1
    res.head.user shouldBe trace.user
    res.head.events should contain theSameElementsInOrderAs Seq(
      Event(Me, Here, Now),
      Event(Me, Here, Now + 10.seconds),
      Event(Me, Here, Now + 25.seconds),
      Event(Me, Here, Now + 44.seconds))
  }

  it should "handle empty traces" in {
    val trace = Trace.empty(Me)
    val res = transform(Seq(trace), 10.seconds)
    res should have size 1
    res.head.user shouldBe trace.user
    res.head.events shouldBe trace.events
  }

  it should "handle singleton traces" in {
    val trace = Trace(Seq(Event(Me, Here, Now)))
    val res = transform(Seq(trace), 10.seconds)
    res should have size 1
    res.head.user shouldBe trace.user
    res.head.events shouldBe trace.events
  }

  private def transform(data: Seq[Trace], duration: Duration) = {
    val ds = write(data: _*)
    val res = new TemporalSamplingOp(env).execute(TemporalSamplingIn(duration = duration, data = ds), ctx)
    read(res.data)
  }
}