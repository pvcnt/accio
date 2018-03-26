package fr.cnrs.liris.common.flags

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[Priority]].
 */
class PrioritySpec extends UnitSpec {
  "Priority" should "be ordered correctly" in {
    Priority.Default < Priority.ComputedDefault shouldBe true
    Priority.ComputedDefault < Priority.RcFile shouldBe true
    Priority.RcFile < Priority.CommandLine shouldBe true
    Priority.CommandLine < Priority.InvocationPolicy shouldBe true
    Priority.InvocationPolicy < Priority.SoftwareRequirement shouldBe true
  }
}
