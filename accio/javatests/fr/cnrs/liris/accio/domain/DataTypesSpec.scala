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

package fr.cnrs.liris.accio.domain

import fr.cnrs.liris.lumos.domain.{RemoteFile, Value}
import fr.cnrs.liris.testing.UnitSpec
import fr.cnrs.liris.util.geo.Distance
import org.joda.time.{Duration, Instant}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.GeneratorDrivenPropertyChecks

/**
 * Unit tests for [[DataTypes]].
 */
class DataTypesSpec extends UnitSpec with GeneratorDrivenPropertyChecks {
  behavior of "DataTypes"

  DataTypes.register()

  implicit def distanceArbitrary: Arbitrary[Distance] = Arbitrary(Gen.posNum[Double].map(v => Distance.meters(v)))

  implicit def durationArbitrary: Arbitrary[Duration] = Arbitrary(Gen.posNum[Long].map(v => new Duration(v)))

  implicit def instantArbitrary: Arbitrary[Instant] = Arbitrary(Gen.posNum[Long].map(v => new Instant(v)))

  it should "encode distances" in {
    forAll { v: Distance =>
      val res = Value.apply(v, DataTypes.Distance)
      res shouldBe a[Value.UserDefined]
      res.asInstanceOf[Value.UserDefined].dataType shouldBe DataTypes.Distance
      res.asInstanceOf[Value.UserDefined].v shouldBe v
    }
  }

  it should "encode durations" in {
    forAll { v: Duration =>
      val res = Value.apply(v, DataTypes.Duration)
      res shouldBe a[Value.UserDefined]
      res.asInstanceOf[Value.UserDefined].dataType shouldBe DataTypes.Duration
      res.asInstanceOf[Value.UserDefined].v shouldBe v
    }
  }

  it should "encode timestamps" in {
    forAll { v: Instant =>
      val res = Value.apply(v, DataTypes.Timestamp)
      res shouldBe a[Value.UserDefined]
      res.asInstanceOf[Value.UserDefined].dataType shouldBe DataTypes.Timestamp
      res.asInstanceOf[Value.UserDefined].v shouldBe v
    }
  }

  it should "cast into distances" in {
    Value.Int(3).cast(DataTypes.Distance).get.v shouldBe Distance.meters(3)
    Value.Float(3.14f).cast(DataTypes.Distance).get.v shouldBe Distance.meters(3.140000104904175) // Indeed...
    Value.Double(3.14).cast(DataTypes.Distance).get.v shouldBe Distance.meters(3.14)
    Value.String("3.14.meters").cast(DataTypes.Distance).get.v shouldBe Distance.meters(3.14)

    Value.Long(3).cast(DataTypes.Distance) shouldBe None
    Value.True.cast(DataTypes.Distance) shouldBe None
    Value.File(RemoteFile("/dev/null")).cast(DataTypes.Distance) shouldBe None
    Value.Dataset(RemoteFile("/dev/null")).cast(DataTypes.Distance) shouldBe None
  }

  it should "cast into durations" in {
    Value.Int(3000).cast(DataTypes.Duration).get.v shouldBe Duration.standardSeconds(3)
    Value.Long(3000L).cast(DataTypes.Duration).get.v shouldBe Duration.standardSeconds(3)
    Value.String("3.seconds").cast(DataTypes.Duration).get.v shouldBe Duration.standardSeconds(3)

    Value.Float(3.14f).cast(DataTypes.Duration) shouldBe None
    Value.Double(3.14).cast(DataTypes.Duration) shouldBe None
    Value.True.cast(DataTypes.Duration) shouldBe None
    Value.File(RemoteFile("/dev/null")).cast(DataTypes.Duration) shouldBe None
    Value.Dataset(RemoteFile("/dev/null")).cast(DataTypes.Duration) shouldBe None
  }

  it should "cast into timestamps" in {
    forAll { v: Long =>
      Value.Long(v).cast(DataTypes.Timestamp).get.v shouldBe new Instant(v)
    }

    Value.Int(3).cast(DataTypes.Timestamp) shouldBe None
    Value.Float(3.14f).cast(DataTypes.Timestamp) shouldBe None
    Value.Double(3.14).cast(DataTypes.Timestamp) shouldBe None
    Value.String("3.14.meters").cast(DataTypes.Timestamp) shouldBe None
    Value.True.cast(DataTypes.Timestamp) shouldBe None
    Value.File(RemoteFile("/dev/null")).cast(DataTypes.Timestamp) shouldBe None
    Value.Dataset(RemoteFile("/dev/null")).cast(DataTypes.Timestamp) shouldBe None
  }
}