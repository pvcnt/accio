package fr.cnrs.liris.accio.core.model

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

class TraceSpec extends UnitSpec with WithTraceGenerator {
  "Trace" should "return its duration" in {
    val t1 = randomTrace(Me, 101, Duration.standardSeconds(10))
    t1.duration shouldBe Duration.standardSeconds(1000)

    val t2 = Trace.empty(Me)
    t2.duration shouldBe Duration.millis(0)
  }

  it should "return its size" in {
    val t1 = randomTrace(Me, 101, Duration.standardSeconds(10))
    t1.size shouldBe 101

    val t2 = Trace.empty(Me)
    t2.size shouldBe 0
  }

  it should "return its user" in {
    val t1 = Trace(Me, Seq(Event(Me, Here, Now)))
    t1.user shouldBe Me

    val t2 = Trace.empty(Me)
    t2.user shouldBe Me
  }
}