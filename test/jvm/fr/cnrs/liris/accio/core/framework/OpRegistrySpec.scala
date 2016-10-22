package fr.cnrs.liris.accio.core.framework

import fr.cnrs.liris.accio.core.api.Operator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[OpRegistry]].
 */
class OpRegistrySpec extends UnitSpec {
  behavior of "OpRegistry"

  it should "check whether an operator is registered" in {
    val registry = new OpRegistry(new ReflectOpMetaReader, Set[Class[_ <: Operator[_, _]]](classOf[NoInputOp], classOf[NoOutputOp]))
    registry.contains("NoInput") shouldBe true
    registry.contains("NoOutput") shouldBe true
    registry.contains("Unknown") shouldBe false
  }

  it should "return registered operators" in {
    val registry = new OpRegistry(new ReflectOpMetaReader, Set[Class[_ <: Operator[_, _]]](classOf[NoInputOp], classOf[NoOutputOp]))
    registry("NoInput").opClass shouldBe classOf[NoInputOp]
    registry("NoOutput").opClass shouldBe classOf[NoOutputOp]
    registry.get("NoInput").map(_.opClass) shouldBe Some(classOf[NoInputOp])
    registry.get("NoOutput").map(_.opClass) shouldBe Some(classOf[NoOutputOp])
  }

  it should "detect unknown operators" in {
    val registry = new OpRegistry(new ReflectOpMetaReader, Set[Class[_ <: Operator[_, _]]](classOf[NoInputOp], classOf[NoOutputOp]))
    registry.get("Unknown") shouldBe None
    a[NoSuchElementException] shouldBe thrownBy {
      registry("Unknown")
    }
  }

  it should "return all registered operators" in {
    val registry = new OpRegistry(new ReflectOpMetaReader, Set[Class[_ <: Operator[_, _]]](classOf[NoInputOp], classOf[NoOutputOp]))
    val ops = registry.ops
    ops should have size 2
    ops.map(_.opClass) should contain theSameElementsAs Set(classOf[NoInputOp], classOf[NoOutputOp])
  }
}