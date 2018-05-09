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

package fr.cnrs.liris.sparkle.io

import fr.cnrs.liris.sparkle.format.{DataType, InternalRow, RowEncoder}
import fr.cnrs.liris.sparkle.{DataType, RowEncoder}
import fr.cnrs.liris.testing.UnitSpec
import org.joda.time.Instant
import org.scalactic.Equality

/**
 * Unit tests for [[RowEncoder]].
 */
class RowEncoderSpec extends UnitSpec {
  behavior of "StructEncoder"

  private implicit val rowEquality = new Equality[InternalRow] {
    override def areEqual(a: InternalRow, b: Any): Boolean =
      b match {
        case row: InternalRow => a.fields.deep == row.fields.deep
        case _ => false
      }
  }

  it should "create an encoder for a case class" in {
    val encoder = RowEncoder[TestStruct]
    encoder.structType.wrapper shouldBe false
    encoder.structType.fields shouldBe Seq(
      "i" -> DataType.Int32,
      "l" -> DataType.Int64,
      "f" -> DataType.Float32,
      "d" -> DataType.Float64,
      "b" -> DataType.Bool,
      "s" -> DataType.String,
      "t" -> DataType.Time)

    val obj = TestStruct(1, 2, 3, 4, true, "foo", new Instant(1234567890))
    val row = InternalRow(Array(1, 2, 3, 4, true, "foo", new Instant(1234567890)))
    encoder.serialize(obj) should ===(row)
    encoder.deserialize(row) shouldBe obj
  }

  it should "create an encoder for a case class with Java types" in {
    val encoder = RowEncoder[TestJavaStruct]
    encoder.structType.wrapper shouldBe false
    encoder.structType.fields shouldBe Seq(
      "i" -> DataType.Int32,
      "l" -> DataType.Int64,
      "f" -> DataType.Float32,
      "d" -> DataType.Float64,
      "b" -> DataType.Bool,
      "s" -> DataType.String)

    val obj = TestJavaStruct(java.lang.Integer.valueOf(1), java.lang.Long.valueOf(2), java.lang.Float.valueOf(3), java.lang.Double.valueOf(4), java.lang.Boolean.TRUE, "foo")
    val row = InternalRow(Array(1, 2, 3, 4, true, "foo"))
    encoder.serialize(obj) should ===(row)
    // TODO: There is a mismatch between Scala types and Java boxed types. But do we need to support it?
    //encoder.deserialize(row) shouldBe obj
  }

  it should "create an encoder for plain values" in {
    val encoder = RowEncoder[Int]
    encoder.structType.wrapper shouldBe true
    encoder.structType.fields shouldBe Seq("value" -> DataType.Int32)
    encoder.serialize(42) should ===(InternalRow(Array(42)))
    encoder.deserialize(InternalRow(Array(42))) shouldBe 42
  }

  it should "create an encoder for a static inner case class" in {
    val encoder = RowEncoder[OuterClass.InnerStruct2]
    encoder.structType.wrapper shouldBe false
    encoder.structType.fields shouldBe Seq("i" -> DataType.Int32)
    encoder.serialize(OuterClass.InnerStruct2(42)) should ===(InternalRow(Array(42)))
    encoder.deserialize(InternalRow(Array(42))) shouldBe OuterClass.InnerStruct2(42)
  }

  it should "create an encoder for a parametrized case class" in {
    val encoder = RowEncoder[TestParametrizedStruct[String]]
    encoder.structType.wrapper shouldBe false
    encoder.structType.fields shouldBe Seq("i" -> DataType.Int32, "o" -> DataType.String)
    val obj = TestParametrizedStruct(42, "foo")
    val row = InternalRow(Array(42, "foo"))
    encoder.serialize(obj) should ===(row)
    encoder.deserialize(row) shouldBe obj
  }

  it should "reject a non-static inner class" in {
    val e = intercept[RuntimeException](RowEncoder[OuterClass#InnerStruct])
    e.getMessage shouldBe "Non-static inner class is not supported: fr.cnrs.liris.sparkle.io.OuterClass.InnerStruct"
  }

  it should "reject an invalid type" in {
    val e = intercept[RuntimeException](RowEncoder[TestInvalidType])
    e.getMessage shouldBe "Unsupported Scala type at fr.cnrs.liris.sparkle.io.TestInvalidType.t: java.sql.Timestamp"
  }
}