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

import java.sql.Timestamp

import fr.cnrs.liris.lumos.domain.Arbitraries._
import fr.cnrs.liris.testing.UnitSpec
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary._
import org.scalatest.prop.GeneratorDrivenPropertyChecks

import scala.reflect.{ClassTag, classTag}
import scala.util.Try

/**
 * Unit tests for [[Value]].
 */
class ValueSpec extends UnitSpec with GeneratorDrivenPropertyChecks {
  behavior of "Value"

  private val genTimestamp = arbitrary[Long].map(v => new Timestamp(v))

  implicit private def timestampArbitrary: Arbitrary[Timestamp] = Arbitrary(genTimestamp)

  object TimestampType extends DataType.UserDefined {
    override type JvmType = Timestamp

    override def name = "Timestamp"

    override def help = "a timestamp"

    override def cls: ClassTag[this.JvmType] = classTag[Timestamp]

    override def encode(v: this.JvmType): Value = Value.Long(v.getTime)

    override def decode(value: Value): Option[this.JvmType] =
    value match {
      case Value.Long(v) => Some(new Timestamp(v))
      case Value.String(v) => Try(v.toLong).toOption.map(v => new Timestamp(v))
      case _ => None
    }
  }

  it should "encode strings" in {
    forAll { v: String => Value.apply(v, DataType.String) shouldBe Value.String(v) }
  }

  it should "encode integers" in {
    forAll { v: Int => Value.apply(v, DataType.Int) shouldBe Value.Int(v) }
  }

  it should "encode longs" in {
    forAll { v: Long => Value.apply(v, DataType.Long) shouldBe Value.Long(v) }
  }

  it should "encode floats" in {
    forAll { v: Float => Value.apply(v, DataType.Float) shouldBe Value.Float(v) }
  }

  it should "encode doubles" in {
    forAll { v: Double => Value.apply(v, DataType.Double) shouldBe Value.Double(v) }
  }

  it should "encode booleans" in {
    Value.apply(true, DataType.Bool) shouldBe Value.True
    Value.apply(false, DataType.Bool) shouldBe Value.False
  }

  it should "encode files" in {
    forAll { v: RemoteFile => Value.apply(v, DataType.File) shouldBe Value.File(v) }
  }

  it should "encode datasets" in {
    forAll { v: RemoteFile => Value.apply(v, DataType.Dataset) shouldBe Value.Dataset(v) }
  }

  it should "encode custom types" in {
    forAll { v: Timestamp => Value.apply(v, TimestampType) shouldBe Value.UserDefined(v, TimestampType) }
  }

  it should "cast strings into other types" in {
    Value.String("3.14").cast(DataType.Float) shouldBe Some(Value.Float(3.14f))
    Value.String("foo").cast(DataType.Float) shouldBe None

    Value.String("3.14").cast(DataType.Double) shouldBe Some(Value.Double(3.14))
    Value.String("foo").cast(DataType.Double) shouldBe None

    Value.String("42").cast(DataType.Int) shouldBe Some(Value.Int(42))
    Value.String("foo").cast(DataType.Int) shouldBe None

    Value.String("992659245677255065").cast(DataType.Long) shouldBe Some(Value.Long(992659245677255065L))
    Value.String("foo").cast(DataType.Long) shouldBe None

    Value.String("true").cast(DataType.Bool) shouldBe Some(Value.True)
    Value.String("t").cast(DataType.Bool) shouldBe Some(Value.True)
    Value.String("1").cast(DataType.Bool) shouldBe Some(Value.True)
    Value.String("false").cast(DataType.Bool) shouldBe Some(Value.False)
    Value.String("f").cast(DataType.Bool) shouldBe Some(Value.False)
    Value.String("0").cast(DataType.Bool) shouldBe Some(Value.False)
    Value.String("foo").cast(DataType.Bool) shouldBe None

    Value.String("foo").cast(DataType.String) shouldBe Some(Value.String("foo"))

    Value.String("foo").cast(DataType.Dataset) shouldBe None
    Value.String("foo").cast(DataType.File) shouldBe None
  }

  it should "cast integers into other types" in {
    Value.Int(42).cast(DataType.Float) shouldBe Some(Value.Float(42f))
    Value.Int(42).cast(DataType.Double) shouldBe Some(Value.Double(42d))
    Value.Int(42).cast(DataType.Long) shouldBe Some(Value.Long(42L))
    Value.Int(42).cast(DataType.String) shouldBe Some(Value.String("42"))
    Value.Int(42).cast(DataType.Int) shouldBe Some(Value.Int(42))

    Value.Int(42).cast(DataType.Bool) shouldBe None
    Value.Int(42).cast(DataType.Dataset) shouldBe None
    Value.Int(42).cast(DataType.File) shouldBe None
  }

  it should "cast longs into other types" in {
    forAll { l: Long =>
      val v = Value.Long(l)
      v.cast(DataType.String).get.cast(DataType.Long).get shouldBe v
      v.cast(DataType.Long).get.cast(DataType.Long).get shouldBe v

      v.cast(DataType.Double) shouldBe None
      v.cast(DataType.Float) shouldBe None
      v.cast(DataType.Int) shouldBe None
      v.cast(DataType.Bool) shouldBe None
      v.cast(DataType.Dataset) shouldBe None
      v.cast(DataType.File) shouldBe None
    }
  }

  it should "cast floats into other types" in {
    Value.Float(3.14f).cast(DataType.Double) shouldBe Some(Value.Double(3.140000104904175)) // Indeed...
    Value.Float(3.14f).cast(DataType.String) shouldBe Some(Value.String("3.14"))
    Value.Float(3.14f).cast(DataType.Float) shouldBe Some(Value.Float(3.14f))

    Value.Float(3.14f).cast(DataType.Long) shouldBe None
    Value.Float(3.14f).cast(DataType.Int) shouldBe None
    Value.Float(3.14f).cast(DataType.Bool) shouldBe None
    Value.Float(3.14f).cast(DataType.Dataset) shouldBe None
    Value.Float(3.14f).cast(DataType.File) shouldBe None
  }

  it should "cast doubles into other types" in {
    Value.Double(3.14).cast(DataType.String) shouldBe Some(Value.String("3.14"))
    Value.Double(3.14).cast(DataType.Double) shouldBe Some(Value.Double(3.14))

    //Value.Double(3.14).cast(DataType.Float) shouldBe None
    Value.Double(3.14).cast(DataType.Long) shouldBe None
    Value.Double(3.14).cast(DataType.Int) shouldBe None
    Value.Double(3.14).cast(DataType.Bool) shouldBe None
    Value.Double(3.14).cast(DataType.Dataset) shouldBe None
    Value.Double(3.14).cast(DataType.File) shouldBe None
  }

  it should "cast booleans into other types" in {
    Value.True.cast(DataType.String) shouldBe Some(Value.String("true"))
    Value.False.cast(DataType.String) shouldBe Some(Value.String("false"))
    Value.True.cast(DataType.Bool) shouldBe Some(Value.True)

    Value.True.cast(DataType.Float) shouldBe None
    Value.True.cast(DataType.Double) shouldBe None
    Value.True.cast(DataType.Int) shouldBe None
    Value.True.cast(DataType.Long) shouldBe None
    Value.True.cast(DataType.Dataset) shouldBe None
    Value.True.cast(DataType.File) shouldBe None
  }

  it should "cast datasets into other types" in {
    Value.Dataset(RemoteFile("/path/to/file")).cast(DataType.File) shouldBe Some(Value.File(RemoteFile("/path/to/file")))
    Value.Dataset(RemoteFile("/path/to/file")).cast(DataType.Dataset) shouldBe Some(Value.Dataset(RemoteFile("/path/to/file")))

    Value.Dataset(RemoteFile("/path/to/file")).cast(DataType.String) shouldBe None
    Value.Dataset(RemoteFile("/path/to/file")).cast(DataType.Float) shouldBe None
    Value.Dataset(RemoteFile("/path/to/file")).cast(DataType.Double) shouldBe None
    Value.Dataset(RemoteFile("/path/to/file")).cast(DataType.Int) shouldBe None
    Value.Dataset(RemoteFile("/path/to/file")).cast(DataType.Long) shouldBe None
    Value.Dataset(RemoteFile("/path/to/file")).cast(DataType.Bool) shouldBe None
  }

  it should "cast files into other types" in {
    Value.File(RemoteFile("/path/to/file")).cast(DataType.File) shouldBe Some(Value.File(RemoteFile("/path/to/file")))
    Value.File(RemoteFile("/path/to/file")).cast(DataType.Dataset) shouldBe Some(Value.Dataset(RemoteFile("/path/to/file")))

    Value.File(RemoteFile("/path/to/file")).cast(DataType.String) shouldBe None
    Value.File(RemoteFile("/path/to/file")).cast(DataType.Float) shouldBe None
    Value.File(RemoteFile("/path/to/file")).cast(DataType.Double) shouldBe None
    Value.File(RemoteFile("/path/to/file")).cast(DataType.Int) shouldBe None
    Value.File(RemoteFile("/path/to/file")).cast(DataType.Long) shouldBe None
    Value.File(RemoteFile("/path/to/file")).cast(DataType.Bool) shouldBe None
  }

  it should "cast timestamps into other types" in {
    forAll { t: Timestamp =>
      val v = Value.UserDefined(t, TimestampType)
      v.cast(DataType.String).get.cast(TimestampType).get shouldBe v
      v.cast(DataType.Long).get.cast(TimestampType).get shouldBe v

      v.cast(DataType.Float) shouldBe None
      v.cast(DataType.Double) shouldBe None
      v.cast(DataType.Int) shouldBe None
      v.cast(DataType.Bool) shouldBe None
      v.cast(DataType.Dataset) shouldBe None
      v.cast(DataType.File) shouldBe None
    }
  }
}