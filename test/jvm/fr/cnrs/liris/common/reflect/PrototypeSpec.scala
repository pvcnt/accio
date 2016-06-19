package fr.cnrs.liris.common.reflect

import fr.cnrs.liris.testing.UnitSpec

import scala.reflect.runtime.universe._

/**
 * Unit tests for [[Prototype]].
 */
class PrototypeSpec extends UnitSpec {
  "Prototype" should "contain a name and a tag when created manually" in {
    val proto = Prototype("foo", typeTag[Int])
    proto.name shouldBe "foo"
    proto.tag shouldBe typeTag[Int]
  }

  it should "contain a name and a tag when created with a type" in {
    val proto = Prototype("foo", typeTag[Int])
    proto.name shouldBe "foo"
    proto.tag shouldBe typeTag[Int]
  }

  it should "equal another Prototype with the same name and tag" in {
    Prototype("foo", typeTag[Int]) shouldEqual Prototype("foo", typeTag[Int])
  }

  it should "not equal another Prototype with a different name" in {
    Prototype("foo", typeTag[Int]) shouldNot equal(Prototype("bar", typeTag[Int]))
  }

  it should "not equal another Prototype with a different tag" in {
    Prototype("foo", typeTag[Int]) shouldNot equal(Prototype("foo", typeTag[Double]))
  }

  it should "be boundable to a given value" in {
    val proto = Prototype("foo", typeTag[Int])
    val value = proto.bind(42)
    value.proto shouldBe proto
    value.value shouldBe 42
  }

  "Value" should "contain a prototype and a literal value" in {
    val proto = Prototype("foo", typeTag[Int])
    val value = Value(proto, 42)
    value.proto shouldBe proto
    value.value shouldBe 42
  }
}