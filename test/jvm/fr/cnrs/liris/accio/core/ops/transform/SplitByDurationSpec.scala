package fr.cnrs.liris.accio.core.ops.transform

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

import scala.util.Random

class SplitByDurationSpec extends UnitSpec with WithTraceGenerator {
  "SplitByDuration" should "split by duration" in {
    val trace = randomTrace(Me, 150, Duration.standardSeconds(Random.nextInt(10)))
    val res = DurationSplittingOp(Duration.standardSeconds(10)).transform(trace)
    res.map(_.user).foreach(user => user shouldBe Me)
    println(res.flatMap(_.events).size)
    println(trace.events.size  )
    res.flatMap(_.events) should contain theSameElementsInOrderAs trace.events
    res.foreach(_.duration.seconds should be <= (10L))
  }

  it should "handle a duration greater than trace's duration" in {
    val trace = randomTrace(Me, 60, Duration.standardSeconds(1))
    val res = DurationSplittingOp(Duration.standardSeconds(100)).transform(trace)
    res should have size 1
    res.head.user shouldBe trace.user
    res.head.events should contain theSameElementsInOrderAs trace.events
  }
}