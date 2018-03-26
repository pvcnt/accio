package fr.cnrs.liris.common.flags

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[TriState]].
 */
class TriStateSpec extends UnitSpec {
  behavior of "TriState"

  it should "return a parsable string representation" in {
    TriStateConverter.convert(TriState.Yes.toString) shouldBe TriState.Yes
    TriStateConverter.convert(TriState.No.toString) shouldBe TriState.No
    TriStateConverter.convert(TriState.Auto.toString) shouldBe TriState.Auto
  }
}