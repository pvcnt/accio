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

package fr.cnrs.liris.sparkle

import fr.cnrs.liris.sparkle.format.{DataType, InternalRow}
import fr.cnrs.liris.testing.UnitSpec
import org.joda.time.Instant
import org.scalactic.Equality

/**
 * Unit tests for [[Encoder]].
 */
class EncoderSpec extends UnitSpec {
  behavior of "Encoder"

  private implicit val rowEquality = new Equality[InternalRow] {
    override def areEqual(a: InternalRow, b: Any): Boolean =
      b match {
        case row: InternalRow => a.fields.deep == row.fields.deep
        case _ => false
      }
  }

  it should "create an encoder for a case class" in {
    val encoder = Encoder[TestStruct]
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
    val res = encoder.serialize(obj)
    res should have size 1
    res.head should ===(row)
    encoder.deserialize(row) shouldBe obj
  }

  it should "create an encoder for a case class with Java types" in {
    val encoder = Encoder[TestJavaStruct]
    encoder.structType.fields shouldBe Seq(
      "i" -> DataType.Int32,
      "l" -> DataType.Int64,
      "f" -> DataType.Float32,
      "d" -> DataType.Float64,
      "b" -> DataType.Bool,
      "s" -> DataType.String)

    val obj = TestJavaStruct(java.lang.Integer.valueOf(1), java.lang.Long.valueOf(2), java.lang.Float.valueOf(3), java.lang.Double.valueOf(4), java.lang.Boolean.TRUE, "foo")
    val row = InternalRow(Array(1, 2, 3, 4, true, "foo"))
    val res = encoder.serialize(obj)
    res should have size 1
    res.head should ===(row)
    // TODO: There is a mismatch between Scala types and Java boxed types. But do we need to support it?
    //encoder.deserialize(row) shouldBe obj
  }

  it should "create an encoder for plain values" in {
    val encoder = Encoder[Int]
    encoder.structType.fields shouldBe Seq("value" -> DataType.Int32)
    val res = encoder.serialize(42)
    res should have size 1
    res.head should ===(InternalRow(Array(42)))
    encoder.deserialize(InternalRow(Array(42))) shouldBe 42
  }

  it should "create an encoder for a static inner case class" in {
    val encoder = Encoder[OuterClass.InnerStruct2]
    encoder.structType.fields shouldBe Seq("i" -> DataType.Int32)
    val res = encoder.serialize(OuterClass.InnerStruct2(42))
    res should have size 1
    res.head should ===(InternalRow(Array(42)))
    encoder.deserialize(InternalRow(Array(42))) shouldBe OuterClass.InnerStruct2(42)
  }

  it should "create an encoder for a parametrized case class" in {
    val encoder = Encoder[TestParametrizedStruct[String]]
    encoder.structType.fields shouldBe Seq("i" -> DataType.Int32, "o" -> DataType.String)
    val obj = TestParametrizedStruct(42, "foo")
    val row = InternalRow(Array(42, "foo"))

    val res = encoder.serialize(obj)
    res should have size 1
    res.head should ===(row)
    encoder.deserialize(row) shouldBe obj
  }

  it should "reject a non-static inner class" in {
    val e = intercept[RuntimeException](Encoder[OuterClass#InnerStruct])
    e.getMessage shouldBe "Non-static inner class is not supported: fr.cnrs.liris.sparkle.OuterClass.InnerStruct"
  }

  it should "reject an invalid type" in {
    val e = intercept[RuntimeException](Encoder[TestInvalidType])
    e.getMessage shouldBe "Unsupported Scala type at fr.cnrs.liris.sparkle.TestInvalidType.t: java.sql.Timestamp"
  }
}