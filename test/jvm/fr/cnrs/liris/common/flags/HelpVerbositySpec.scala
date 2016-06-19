package fr.cnrs.liris.common.flags

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[HelpVerbosity]].
 */
class HelpVerbositySpec extends UnitSpec {
  "HelpVerbosity" should "be ordered correctly" in {
    HelpVerbosity.Short < HelpVerbosity.Medium shouldBe true
    HelpVerbosity.Medium < HelpVerbosity.Long shouldBe true
  }
}
