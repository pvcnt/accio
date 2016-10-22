package fr.cnrs.liris.accio.ops

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.privamov.model.Trace
import fr.cnrs.liris.privamov.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

import scala.util.Random

class DurationSplittingOpSpec extends UnitSpec with WithTraceGenerator with WithSparkleEnv {
  behavior of "DurationSplittingOp"

  it should "split by duration" in {
    val trace = randomTrace(Me, 150, Duration.standardSeconds(Random.nextInt(10)))
    val res = transform(Seq(trace), Duration.standardSeconds(10))
    res.map(_.user).foreach(user => user shouldBe Me)
    res.flatMap(_.events) should contain theSameElementsInOrderAs trace.events
    res.foreach(_.duration.seconds should be <= (10L))
  }

  it should "handle a duration greater than trace's duration" in {
    val trace = randomTrace(Me, 60, Duration.standardSeconds(1))
    val res = transform(Seq(trace), Duration.standardSeconds(100))
    res should have size 1
    res.head.user shouldBe trace.user
    res.head.events should contain theSameElementsInOrderAs trace.events
  }

  private def transform(data: Seq[Trace], duration: Duration) = {
    val ds = write(data: _*)
    val res = new DurationSplittingOp(env).execute(DurationSplittingIn(duration = duration, data = ds), ctx)
    read(res.data)
  }
}