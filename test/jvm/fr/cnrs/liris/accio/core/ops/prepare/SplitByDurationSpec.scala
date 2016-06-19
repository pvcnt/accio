package fr.cnrs.liris.accio.core.ops.transform

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.core.framework.BoundTransformer
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

import scala.util.Random

class SplitByDurationSpec extends UnitSpec with WithTraceGenerator {
  "SplitByDuration" should "split by duration" in {
    val trace = randomTrace(Me, 150, Duration.standardSeconds(Random.nextInt(10)))
    val res = transform(trace, Duration.standardSeconds(10))
    res.map(_.user).foreach(user => user shouldBe Me)
    res.flatMap(_.records) shouldBe trace.records
    res.foreach(_.duration.seconds should be <= (10L))
  }

  it should "handle a duration greater than trace's duration" in {
    val trace = randomTrace(Me, 60, Duration.standardSeconds(1))
    val res = transform(trace, Duration.standardSeconds(100))
    res should have size 1
    res.head shouldBe trace
  }

  private def transform(trace: Trace, duration: Duration) = {
    val transformation = BoundTransformer(new SplitByDuration)(_.duration := duration)
    transformation.transform(trace)
  }
}