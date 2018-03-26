package fr.cnrs.liris.common.reflect

import javax.annotation.{CheckReturnValue, Nullable}

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[CaseClass]].
 */
class CaseClassSpec extends UnitSpec {
  "CaseClass" should "provide information about case class" in {
    val reflClass = CaseClass[SomeCaseClass]

    reflClass.runtimeClass shouldEqual classOf[SomeCaseClass]
    reflClass.isAnnotated[Nullable] shouldBe false
    reflClass.isAnnotated[CheckReturnValue] shouldBe true
  }

  it should "provide information about fields" in {
    val reflClass = CaseClass[SomeCaseClass]

    reflClass.fields.size shouldBe 3

    reflClass.fields(0).name shouldBe "i"
    reflClass.fields(0).scalaType.runtimeClass shouldBe classOf[Option[_]]
    reflClass.fields(0).scalaType.typeArguments.head.runtimeClass shouldBe classOf[java.lang.Integer]
    reflClass.fields(0).defaultValue shouldBe Some(None)
    reflClass.fields(0).isAnnotated[CheckReturnValue] shouldBe false

    reflClass.fields(1).name shouldBe "j"
    reflClass.fields(1).scalaType.runtimeClass shouldBe classOf[java.lang.Double]
    reflClass.fields(1).defaultValue shouldBe None
    reflClass.fields(1).annotation[Nullable] shouldBe a[Nullable]
    reflClass.fields(1).isAnnotated[CheckReturnValue] shouldBe false

    reflClass.fields(2).name shouldBe "s"
    reflClass.fields(2).scalaType.runtimeClass shouldBe classOf[String]
    reflClass.fields(2).defaultValue shouldBe Some("foobar")
    reflClass.fields(2).isAnnotated[CheckReturnValue] shouldBe false
  }

  it should "not support multiple constructors case classes" in {
    val expected = intercept[IllegalArgumentException] {
      CaseClass[MultipleConstructorCaseClass]
    }
    expected.getMessage shouldBe "requirement failed: Multiple constructors case classes are not supported"
  }

  it should "not support non-static inner case classes" in {
    val expected = intercept[IllegalArgumentException] {
      CaseClass[CaseClassContainer#ContainedCaseClass]
    }
    expected.getMessage shouldBe "requirement failed: Non-static inner case classes are not supported"
  }
}

@CheckReturnValue
case class SomeCaseClass(i: Option[Int], @Nullable j: Double, s: String = "foobar")

class CaseClassContainer(str: String) {

  case class ContainedCaseClass(i: Int)

}

case class MultipleConstructorCaseClass(i: Int) {
  def this(s: String) = this(s.toInt)
}
