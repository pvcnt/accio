package fr.cnrs.liris.privamov.model

import fr.cnrs.liris.privamov.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[Event]].
 */
class EventSpec extends UnitSpec with WithTraceGenerator {
  "Event" should "set new properties" in {
    val rec1 = Event(Me, Here, Now, Map("foo" -> 1d, "bar" -> 1d))
    val rec2 = rec1.set("foo", 2d)
    val rec3 = rec2.set("foobar", 3d)
    rec1.props("foo") shouldBe 1d
    rec1.props("bar") shouldBe 1d
    rec2.props("foo") shouldBe 2d
    rec2.props("bar") shouldBe 1d
    rec2.props.get("foobar") shouldBe None
    rec3.props("foo") shouldBe 2d
    rec3.props("bar") shouldBe 1d
    rec3.props("foobar") shouldBe 3d
  }
}