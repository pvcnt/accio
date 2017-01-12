/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.domain

import fr.cnrs.liris.accio.core.api.Dataset
import fr.cnrs.liris.common.geo.{Distance, LatLng}
import fr.cnrs.liris.testing.UnitSpec
import org.joda.time.{Duration, Instant}

/**
 * Unit tests for [[Values]].
 */
class ValuesSpec extends UnitSpec {
  behavior of "Values"

  it should "parse strings as byte" in {
    Values.parseByte("1") shouldBe 1
    Values.parse("1", DataType(AtomicType.Byte)) shouldBe 1
  }

  it should "parse strings as integer" in {
    Values.parseInteger("42") shouldBe 42
    Values.parse("42", DataType(AtomicType.Integer)) shouldBe 42
  }

  it should "parse strings as long" in {
    Values.parseLong("992659245677255065") shouldBe 992659245677255065L
    Values.parse("992659245677255065", DataType(AtomicType.Long)) shouldBe 992659245677255065L
  }

  it should "parse strings as double" in {
    Values.parseDouble("3.14") shouldBe 3.14
    Values.parse("3.14", DataType(AtomicType.Double)) shouldBe 3.14
  }

  it should "parse strings as boolean" in {
    Set("true", "t", "yes", "y").foreach { s =>
      Values.parseBoolean(s) shouldBe true
      Values.parse(s, DataType(AtomicType.Boolean)) shouldBe true
    }
    Set("false", "f", "no", "n").foreach { s =>
      Values.parseBoolean(s) shouldBe false
      Values.parse(s, DataType(AtomicType.Boolean)) shouldBe false
    }
  }

  it should "parse strings as string" in {
    Values.parseString("some string") shouldBe "some string"
    Values.parse("some string", DataType(AtomicType.String)) shouldBe "some string"
  }

  it should "parse strings as duration" in {
    Values.parseDuration("3.minutes") shouldBe new Duration(3 * 60 * 1000)
    Values.parse("3.minutes", DataType(AtomicType.Duration)) shouldBe new Duration(3 * 60 * 1000)
  }

  it should "parse strings as distance" in {
    Values.parseDistance("3.14.meters") shouldBe Distance.meters(3.14)
    Values.parse("3.14.meters", DataType(AtomicType.Distance)) shouldBe Distance.meters(3.14)
  }

  it should "parse strings as timestamp" in {
    Values.parseTimestamp("2014-01-02T23:58:29Z") shouldBe new Instant(1388707109000L)
    Values.parse("2014-01-02T23:58:29Z", DataType(AtomicType.Timestamp)) shouldBe new Instant(1388707109000L)
  }

  it should "parse strings as location" in {
    Values.parseLocation("42.4,-12.01") shouldBe LatLng.degrees(42.4, -12.01)
    Values.parse("42.4,-12.01", DataType(AtomicType.Location)) shouldBe LatLng.degrees(42.4, -12.01)
  }

  it should "parse strings as dataset" in {
    Values.parseDataset("/dev/null") shouldBe Dataset("/dev/null")
    Values.parse("/dev/null", DataType(AtomicType.Dataset)) shouldBe Dataset("/dev/null")
  }
}