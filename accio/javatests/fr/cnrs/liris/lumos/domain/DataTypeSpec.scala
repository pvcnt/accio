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

package fr.cnrs.liris.lumos.domain

import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfterEach

import scala.reflect.{ClassTag, classTag}

/**
 * Unit tests for [[DataType]].
 */
class DataTypeSpec extends UnitSpec with BeforeAndAfterEach {
  behavior of "DataType"

  override def beforeEach(): Unit = {
    super.beforeEach()
    DataType.clear()
  }

  it should "retrieve the value of" in {
    DataType.register(TestCustomType)
    DataType.values should have size 9
    DataType.parse("Int") shouldBe Some(DataType.Int)
    DataType.parse("Long") shouldBe Some(DataType.Long)
    DataType.parse("Float") shouldBe Some(DataType.Float)
    DataType.parse("Double") shouldBe Some(DataType.Double)
    DataType.parse("String") shouldBe Some(DataType.String)
    DataType.parse("Bool") shouldBe Some(DataType.Bool)
    DataType.parse("Dataset") shouldBe Some(DataType.Dataset)
    DataType.parse("File") shouldBe Some(DataType.File)
    DataType.parse("Test") shouldBe Some(TestCustomType)
    DataType.parse("Foobar") shouldBe None
  }

  object TestCustomType extends DataType.UserDefined {
    override type JvmType = String

    override def cls: ClassTag[this.JvmType] = classTag[Predef.String]

    override def name: String = "Test"

    override def help = "a test type"

    override def encode(v: this.JvmType): Value = ???

    override def decode(value: Value): Option[this.JvmType] = ???
  }

}
