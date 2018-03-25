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

package fr.cnrs.liris.accio.api

import fr.cnrs.liris.accio.api.thrift.{AtomicType, DataType}
import fr.cnrs.liris.accio.sdk.Dataset
import fr.cnrs.liris.common.geo.{Distance, LatLng}
import fr.cnrs.liris.testing.UnitSpec
import org.joda.time.{Duration, Instant}

/**
 * Unit tests for [[Values]].
 */
class ValuesSpec extends UnitSpec {
  behavior of "Values"

  /**
   * Parse.
   */
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

  /**
   * Encode/decode.
   */
  it should "encode/decode bytes" in {
    Values.decodeByte(Values.encodeByte(3)) shouldBe 3
    Values.decode(Values.encodeByte(3), DataType(AtomicType.Byte)) shouldBe 3
  }

  it should "encode/decode integers" in {
    Values.decodeInteger(Values.encodeInteger(42)) shouldBe 42
    Values.decode(Values.encodeInteger(42), DataType(AtomicType.Integer)) shouldBe 42
  }

  it should "encode/decode longs" in {
    Values.decodeLong(Values.encodeLong(992659245677255065L)) shouldBe 992659245677255065L
    Values.decode(Values.encodeLong(992659245677255065L), DataType(AtomicType.Long)) shouldBe 992659245677255065L
  }

  it should "encode/decode doubles" in {
    Values.decodeDouble(Values.encodeDouble(3.14)) shouldBe 3.14
    Values.decode(Values.encodeDouble(3.14), DataType(AtomicType.Double)) shouldBe 3.14
  }

  it should "encode/decode strings" in {
    Values.decodeString(Values.encodeString("some string")) shouldBe "some string"
    Values.decode(Values.encodeString("some string"), DataType(AtomicType.String)) shouldBe "some string"
  }

  it should "encode/decode booleans" in {
    Values.decodeBoolean(Values.encodeBoolean(true)) shouldBe true
    Values.decode(Values.encodeBoolean(true), DataType(AtomicType.Boolean)) shouldBe true
    Values.decodeBoolean(Values.encodeBoolean(false)) shouldBe false
    Values.decode(Values.encodeBoolean(false), DataType(AtomicType.Boolean)) shouldBe false
  }

  it should "encode/decode datasets" in {
    val dataset = Dataset("/dev/null")
    Values.decodeDataset(Values.encodeDataset(dataset)) shouldBe dataset
    Values.decode(Values.encodeDataset(dataset), DataType(AtomicType.Dataset)) shouldBe dataset
  }

  it should "encode/decode distances" in {
    val distance = Distance.meters(3.14)
    Values.decodeDistance(Values.encodeDistance(distance)) shouldBe distance
    Values.decode(Values.encodeDistance(distance), DataType(AtomicType.Distance)) shouldBe distance
  }

  it should "encode/decode durations" in {
    val duration = new Duration(3 * 60 * 1000)
    Values.decodeDuration(Values.encodeDuration(duration)) shouldBe duration
    Values.decode(Values.encodeDuration(duration), DataType(AtomicType.Duration)) shouldBe duration
  }

  it should "encode/decode locations" in {
    val location = LatLng.radians(0.7400196, -0.20961404)
    Values.decodeLocation(Values.encodeLocation(location)) shouldBe location
    Values.decode(Values.encodeLocation(location), DataType(AtomicType.Location)) shouldBe location
  }

  it should "encode/decode timestamps" in {
    val instant = new Instant(1388707109000L)
    Values.decodeTimestamp(Values.encodeTimestamp(instant)) shouldBe instant
    Values.decode(Values.encodeTimestamp(instant), DataType(AtomicType.Timestamp)) shouldBe instant
  }

  it should "encode/decode lists of integers" in {
    val list = Seq(1, 2, 3)
    val kind = DataType(AtomicType.List, Seq(AtomicType.Integer))
    Values.decodeList(Values.encodeList(list, kind)) shouldBe list
    Values.decode(Values.encodeList(list, kind), kind) shouldBe list
  }

  it should "encode/decode sets of integers" in {
    val set = Set(1, 2, 3)
    val kind = DataType(AtomicType.Set, Seq(AtomicType.Integer))
    Values.decodeSet(Values.encodeSet(set, kind)) shouldBe set
    Values.decode(Values.encodeSet(set, kind), kind) shouldBe set
  }

  it should "encode/decode maps of string=>integer" in {
    val map = Map("one" -> 1, "two" -> 2, "three" -> 3)
    val kind = DataType(AtomicType.Map, Seq(AtomicType.String, AtomicType.Integer))
    Values.decodeMap(Values.encodeMap(map, kind)) shouldBe map
    Values.decode(Values.encodeMap(map, kind)) shouldBe map
  }

  it should "encode/decode maps of string=>double" in {
    val map = Map("one" -> 1.1, "two" -> 2.2, "three" -> 3.3)
    val kind = DataType(AtomicType.Map, Seq(AtomicType.String, AtomicType.Double))
    Values.decodeMap(Values.encodeMap(map, kind)) shouldBe map
    Values.decode(Values.encodeMap(map, kind)) shouldBe map
  }

  it should "encode/decode maps of string=>location" in {
    val map = Map("one" -> LatLng.radians(0.7400196, -0.20961404), "two" -> LatLng.radians(1.0199704, 0.1832595), "three" -> LatLng.radians(-0.7155849, -0.1291543))
    val kind = DataType(AtomicType.Map, Seq(AtomicType.String, AtomicType.Location))
    Values.decodeMap(Values.encodeMap(map, kind)) shouldBe map
    Values.decode(Values.encodeMap(map, kind)) shouldBe map
  }

  it should "encode/decode maps of string=>dataset" in {
    val map = Map("one" -> Dataset("/data/ds1"), "two" -> Dataset("/data/ds2"), "three" -> Dataset("/data/ds3"))
    val kind = DataType(AtomicType.Map, Seq(AtomicType.String, AtomicType.Dataset))
    Values.decodeMap(Values.encodeMap(map, kind)) shouldBe map
    Values.decode(Values.encodeMap(map, kind)) shouldBe map
  }
}