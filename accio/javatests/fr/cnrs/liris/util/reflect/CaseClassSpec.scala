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

package fr.cnrs.liris.util.reflect

import javax.annotation.{CheckReturnValue, Nullable}

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[CaseClass]].
 */
class CaseClassSpec extends UnitSpec {
  behavior of "StructDescriptor"

  it should "provide information about case class" in {
    val refl = CaseClass.apply[TestCaseClass]

    refl.runtimeClass shouldEqual classOf[TestCaseClass]
    refl.annotations.contains[Nullable] shouldBe false
    refl.annotations.contains[CheckReturnValue] shouldBe true
  }

  it should "provide information about fields" in {
    val refl = CaseClass.apply[TestCaseClass]

    refl.fields should have size 3

    refl.fields(0).name shouldBe "i"
    refl.fields(0).runtimeClass shouldBe classOf[Option[_]]
    refl.fields(0).scalaType.isA[Option[Int]] shouldBe true
    refl.fields(0).scalaType.isOption shouldBe true
    refl.fields(0).defaultValue shouldBe Some(None)
    refl.fields(0).annotations.contains[CheckReturnValue] shouldBe false

    refl.fields(1).name shouldBe "j"
    refl.fields(1).runtimeClass shouldBe classOf[Double]
    refl.fields(1).scalaType.isA[Double] shouldBe true
    refl.fields(1).scalaType.isOption shouldBe false
    refl.fields(1).defaultValue shouldBe None
    refl.fields(1).annotations.get[Nullable].get shouldBe a[Nullable]
    refl.fields(1).annotations.contains[CheckReturnValue] shouldBe false

    refl.fields(2).name shouldBe "s"
    refl.fields(2).runtimeClass shouldBe classOf[String]
    refl.fields(2).scalaType.isA[String] shouldBe true
    refl.fields(2).scalaType.isOption shouldBe false
    refl.fields(2).defaultValue shouldBe Some("foobar")
    refl.fields(2).annotations.contains[CheckReturnValue] shouldBe false
  }

  it should "reject non case classes" in {
    val expected = intercept[IllegalArgumentException](CaseClass[PlainClass])
    expected.getMessage shouldBe "Not a case class: fr.cnrs.liris.util.reflect.PlainClass"
  }

  it should "reject classes with multiple constructors" in {
    val expected = intercept[IllegalArgumentException](CaseClass[MultipleConstructorCaseClass])
    expected.getMessage shouldBe "Case class with multiple constructors is not supported: fr.cnrs.liris.util.reflect.MultipleConstructorCaseClass"
  }

  it should "support static inner case classes" in {
    val refl = CaseClass[CaseClassContainer.ValidCaseClass]
    refl.fields should have size 1
    refl.fields(0).name shouldBe "i"
    refl.fields(0).scalaType.isA[Int] shouldBe true
  }

  it should "support parametrized types" in {
    val refl = CaseClass[ParametrizedType[Int]]
    refl.fields should have size 1
    refl.fields.head.scalaType.isA[Int] shouldBe true
  }

  it should "create new instances" in {
    val refl = CaseClass.apply[TestCaseClass]
    refl.newInstance(Seq(Some(3), 3.14, "bar")) shouldBe TestCaseClass(Some(3), 3.14, "bar")
  }

  it should "reject non-static inner case classes" in {
    val expected = intercept[IllegalArgumentException](CaseClass.apply[CaseClassContainer#InvalidCaseClass])
    expected.getMessage shouldBe "Non-static inner class is not supported: fr.cnrs.liris.util.reflect.CaseClassContainer#InvalidCaseClass"
  }
}
