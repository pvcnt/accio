/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Accio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Accio.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnrs.liris.common.reflect

import javax.annotation.{CheckReturnValue, Nullable}

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[CaseClass]].
 */
class CaseClassSpec extends UnitSpec {
  behavior of "CaseClass"

  it should "provide information about case class" in {
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
