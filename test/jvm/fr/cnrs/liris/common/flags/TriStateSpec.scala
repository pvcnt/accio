package fr.cnrs.liris.common.flags

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[TriState]].
 */
class TriStateSpec extends UnitSpec {
  val converter = new TriStateConverter(new BooleanConverter)

  "TriState" should "return a parsable string representation" in {
    converter.convert(TriState.Yes.toString) shouldBe TriState.Yes
    converter.convert(TriState.No.toString) shouldBe TriState.No
    converter.convert(TriState.Auto.toString) shouldBe TriState.Auto
  }
}
