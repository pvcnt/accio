/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.common.reflect

import javax.annotation.{CheckReturnValue, Nullable}

import fr.cnrs.liris.testing.UnitSpec

import scala.reflect.runtime.universe._

/**
 * Unit tests for [[ReflectCaseClass]].
 */
class ReflectCaseClassSpec extends UnitSpec {
  "ReflectCaseClass" should "reflect case class information" in {
    val reflClass = ReflectCaseClass.of(typeTag[SomeCaseClass])

    reflClass.tpe shouldEqual typeOf[SomeCaseClass]
    reflClass.runtimeClass shouldEqual classOf[SomeCaseClass]
    reflClass.isAnnotated[Nullable] shouldBe false
    reflClass.isAnnotated[CheckReturnValue] shouldBe true
  }

  it should "reflect case fields information" in {
    val reflClass = ReflectCaseClass.of(typeTag[SomeCaseClass])

    reflClass.fields.size shouldBe 3

    reflClass.fields(0).name shouldBe "i"
    reflClass.fields(0).tpe =:= typeOf[Option[Int]] shouldBe true
    reflClass.fields(0).defaultValue shouldBe Some(None)
    reflClass.fields(0).isAnnotated[CheckReturnValue] shouldBe false

    reflClass.fields(1).name shouldBe "j"
    reflClass.fields(1).tpe =:= typeOf[Double] shouldBe true
    reflClass.fields(1).defaultValue shouldBe None
    reflClass.fields(1).annotation[Nullable] shouldBe a[Nullable]
    reflClass.fields(1).isAnnotated[CheckReturnValue] shouldBe false

    reflClass.fields(2).name shouldBe "s"
    reflClass.fields(2).tpe =:= typeOf[String] shouldBe true
    reflClass.fields(2).defaultValue shouldBe Some("foobar")
    reflClass.fields(2).isAnnotated[CheckReturnValue] shouldBe false
  }

  it should "not support multiple constructors case classes" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      ReflectCaseClass.of[MultipleConstructorCaseClass]
    }
  }

  it should "not support non-static inner case classes" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      ReflectCaseClass.of[CaseClassContainer#ContainedCaseClass]
    }
  }
}

@CheckReturnValue
case class SomeCaseClass(i: Option[Int], @Nullable j: Double, s: String = "foobar")

case class MultipleConstructorCaseClass(i: Int) {
  def this(s: String) = this(s.toInt)
}

class CaseClassContainer(str: String) {

  case class ContainedCaseClass(i: Int)

}