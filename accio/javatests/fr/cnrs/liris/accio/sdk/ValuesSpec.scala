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

package fr.cnrs.liris.accio.sdk

import fr.cnrs.liris.lumos.domain.Arbitraries._
import fr.cnrs.liris.lumos.domain.{DataType, RemoteFile, Value}
import fr.cnrs.liris.testing.UnitSpec
import fr.cnrs.liris.util.geo.{Distance, Location}
import org.joda.time.{Duration, Instant}
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.prop.GeneratorDrivenPropertyChecks

import scala.reflect.runtime.universe.typeOf

/**
 * Unit tests for [[Values]].
 */
class ValuesSpec extends UnitSpec with GeneratorDrivenPropertyChecks {
  behavior of "Values"

  implicit def distanceArbitrary: Arbitrary[Distance] = Arbitrary(arbitrary[Double].map(v => Distance.meters(v)))

  implicit def durationArbitrary: Arbitrary[Duration] = Arbitrary(arbitrary[Long].map(v => new Duration(v)))

  implicit def instantArbitrary: Arbitrary[Instant] = Arbitrary(arbitrary[Long].map(v => new Instant(v)))

  it should "handle integers" in {
    Values.dataTypeOf(typeOf[Int]) shouldBe(DataType.Int, Set.empty)
    forAll { v: Int =>
      Values.encode(v, DataType.Int) shouldBe Some(Value.Int(v))
    }
  }

  it should "handle longs" in {
    Values.dataTypeOf(typeOf[Long]) shouldBe(DataType.Long, Set.empty)
    forAll { v: Long =>
      Values.encode(v, DataType.Long) shouldBe Some(Value.Long(v))
      Values.decode(Values.encode(v, DataType.Long).get, DataType.Long) shouldBe Some(v)
    }
  }

  it should "handle floats" in {
    Values.dataTypeOf(typeOf[Float]) shouldBe(DataType.Float, Set.empty)
    forAll { v: Float =>
      Values.encode(v, DataType.Float) shouldBe Some(Value.Float(v))
      Values.decode(Values.encode(v, DataType.Float).get, DataType.Float) shouldBe Some(v)
    }
  }

  it should "handle doubles" in {
    Values.dataTypeOf(typeOf[Double]) shouldBe(DataType.Double, Set.empty)
    forAll { v: Double =>
      Values.encode(v, DataType.Double) shouldBe Some(Value.Double(v))
      Values.decode(Values.encode(v, DataType.Double).get, DataType.Double) shouldBe Some(v)
    }
  }

  it should "handle strings" in {
    Values.dataTypeOf(typeOf[String]) shouldBe(DataType.String, Set.empty)
    forAll { v: String =>
      Values.encode(v, DataType.String) shouldBe Some(Value.String(v))
      Values.decode(Values.encode(v, DataType.String).get, DataType.String) shouldBe Some(v)
    }
  }

  it should "handle booleans" in {
    Values.dataTypeOf(typeOf[Boolean]) shouldBe(DataType.Bool, Set.empty)
    forAll { v: Boolean =>
      Values.encode(v, DataType.Bool) shouldBe Some(Value.Bool(v))
      Values.decode(Values.encode(v, DataType.Bool).get, DataType.Bool) shouldBe Some(v)
    }
  }

  it should "handle files" in {
    Values.dataTypeOf(typeOf[RemoteFile]) shouldBe(DataType.File, Set.empty)
    forAll { v: RemoteFile =>
      Values.encode(v, DataType.File) shouldBe Some(Value.File(v))
      Values.decode(Values.encode(v, DataType.File).get, DataType.File) shouldBe Some(v)
    }
  }

  it should "handle distances" in {
    Values.dataTypeOf(typeOf[Distance]) shouldBe(DataType.Double, Set("distance"))
    forAll { v: Distance =>
      Values.encode(v, DataType.Double, Set("distance")) shouldBe Some(Value.Double(v.meters))
      Values.decode(Values.encode(v, DataType.Double, Set("distance")).get, DataType.Double, Set("distance")) shouldBe Some(v)
    }
  }

  it should "handle timestamps" in {
    Values.dataTypeOf(typeOf[Instant]) shouldBe(DataType.Long, Set("time"))
    forAll { v: Instant =>
      Values.encode(v, DataType.Long, Set("time")) shouldBe Some(Value.Long(v.getMillis))
      Values.decode(Values.encode(v, DataType.Long, Set("time")).get, DataType.Long, Set("time")) shouldBe Some(v)
    }
  }

  it should "handle durations" in {
    Values.dataTypeOf(typeOf[Duration]) shouldBe(DataType.Long, Set("duration"))
    forAll { v: Duration =>
      Values.encode(v, DataType.Long, Set("duration")) shouldBe Some(Value.Long(v.getMillis))
      Values.decode(Values.encode(v, DataType.Long, Set("duration")).get, DataType.Long, Set("duration")) shouldBe Some(v)
    }
  }

  it should "reject an invalid type" in {
    val e = intercept[IllegalArgumentException](Values.dataTypeOf(typeOf[Location]))
    e.getMessage shouldBe "Unsupported Scala type: fr.cnrs.liris.util.geo.Location"
  }

}
