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

import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.sdk.Dataset
import fr.cnrs.liris.testing.UnitSpec
import fr.cnrs.liris.util.geo.{Distance, LatLng}
import org.joda.time.{Duration, Instant}

/**
 * Unit tests for [[Values]].
 */
class ValuesSpec extends UnitSpec {
  behavior of "Values"

  /**
   * Parse.
   */
  it should "parse strings as float" in {
    Values.parseFloat("3.14") shouldBe Some(Values.encodeFloat(3.14f))
    Values.parse("3.14", AtomicType.Float) shouldBe Some(Values.encodeFloat(3.14f))
  }

  it should "parse strings as integer" in {
    Values.parseInteger("42") shouldBe Some(Values.encodeInteger(42))
    Values.parse("42", AtomicType.Integer) shouldBe Some(Values.encodeInteger(42))
  }

  it should "parse strings as long" in {
    Values.parseLong("992659245677255065") shouldBe Some(Values.encodeLong(992659245677255065L))
    Values.parse("992659245677255065", AtomicType.Long) shouldBe Some(Values.encodeLong(992659245677255065L))
  }

  it should "parse strings as double" in {
    Values.parseDouble("3.14") shouldBe Some(Values.encodeDouble(3.14))
    Values.parse("3.14", AtomicType.Double) shouldBe Some(Values.encodeDouble(3.14))
  }

  it should "parse strings as boolean" in {
    Set("true", "t", "yes", "y").foreach { s =>
      Values.parseBoolean(s) shouldBe Some(Values.encodeBoolean(true))
      Values.parse(s, AtomicType.Boolean) shouldBe Some(Values.encodeBoolean(true))
    }
    Set("false", "f", "no", "n").foreach { s =>
      Values.parseBoolean(s) shouldBe Some(Values.encodeBoolean(false))
      Values.parse(s, AtomicType.Boolean) shouldBe Some(Values.encodeBoolean(false))
    }
  }

  it should "parse strings as string" in {
    Values.parseString("some string") shouldBe Some(Values.encodeString("some string"))
    Values.parse("some string", AtomicType.String) shouldBe Some(Values.encodeString("some string"))
  }

  it should "parse strings as duration" in {
    Values.parseDuration("3.minutes") shouldBe Some(Values.encodeDuration(new Duration(3 * 60 * 1000)))
    Values.parse("3.minutes", AtomicType.Duration) shouldBe Some(Values.encodeDuration(new Duration(3 * 60 * 1000)))
  }

  it should "parse strings as distance" in {
    Values.parseDistance("3.14.meters") shouldBe Some(Values.encodeDistance(Distance.meters(3.14)))
    Values.parse("3.14.meters", AtomicType.Distance) shouldBe Some(Values.encodeDistance(Distance.meters(3.14)))
  }

  it should "parse strings as timestamp" in {
    Values.parseTimestamp("2014-01-02T23:58:29Z") shouldBe Some(Values.encodeTimestamp(new Instant(1388707109000L)))
    Values.parse("2014-01-02T23:58:29Z", AtomicType.Timestamp) shouldBe Some(Values.encodeTimestamp(new Instant(1388707109000L)))
  }

  it should "parse strings as location" in {
    Values.parseLocation("42.4,-12.01") shouldBe Some(Values.encodeLocation(LatLng.degrees(42.4, -12.01)))
    Values.parse("42.4,-12.01", AtomicType.Location) shouldBe Some(Values.encodeLocation(LatLng.degrees(42.4, -12.01)))
  }

  /**
   * Encode/decode.
   */
  it should "encode/decode floats" in {
    Values.decodeFloat(Values.encodeFloat(3.14f)) shouldBe 3.14f
    Values.decode(Values.encodeFloat(3.14f)) shouldBe 3.14f
    Values.decode(Values.encodeFloat(3.14f), DataType.Atomic(AtomicType.Float)) shouldBe 3.14f
  }

  it should "encode/decode integers" in {
    Values.decodeInteger(Values.encodeInteger(42)) shouldBe 42
    Values.decode(Values.encodeInteger(42)) shouldBe 42
    Values.decode(Values.encodeInteger(42), DataType.Atomic(AtomicType.Integer)) shouldBe 42
  }

  it should "encode/decode longs" in {
    Values.decodeLong(Values.encodeLong(992659245677255065L)) shouldBe 992659245677255065L
    Values.decode(Values.encodeLong(992659245677255065L)) shouldBe 992659245677255065L
    Values.decode(Values.encodeLong(992659245677255065L), DataType.Atomic(AtomicType.Long)) shouldBe 992659245677255065L
  }

  it should "encode/decode doubles" in {
    Values.decodeDouble(Values.encodeDouble(3.14)) shouldBe 3.14
    Values.decode(Values.encodeDouble(3.14)) shouldBe 3.14
    Values.decode(Values.encodeDouble(3.14), DataType.Atomic(AtomicType.Double)) shouldBe 3.14
  }

  it should "encode/decode strings" in {
    Values.decodeString(Values.encodeString("some string")) shouldBe "some string"
    Values.decode(Values.encodeString("some string")) shouldBe "some string"
    Values.decode(Values.encodeString("some string"), DataType.Atomic(AtomicType.String)) shouldBe "some string"
  }

  it should "encode/decode booleans" in {
    Values.decodeBoolean(Values.encodeBoolean(true)) shouldBe true
    Values.decode(Values.encodeBoolean(true)) shouldBe true
    Values.decode(Values.encodeBoolean(true), DataType.Atomic(AtomicType.Boolean)) shouldBe true

    Values.decodeBoolean(Values.encodeBoolean(false)) shouldBe false
    Values.decode(Values.encodeBoolean(false)) shouldBe false
    Values.decode(Values.encodeBoolean(false), DataType.Atomic(AtomicType.Boolean)) shouldBe false
  }

  it should "encode/decode datasets" in {
    val dataset = Dataset("/dev/null")
    Values.decodeDataset(Values.encodeDataset(dataset), DatasetType()) shouldBe dataset
    Values.decode(Values.encodeDataset(dataset)) shouldBe dataset
    Values.decode(Values.encodeDataset(dataset), DataType.Dataset(DatasetType())) shouldBe dataset
  }

  it should "encode/decode distances" in {
    val distance = Distance.meters(3.14)
    Values.decodeDistance(Values.encodeDistance(distance)) shouldBe distance
    Values.decode(Values.encodeDistance(distance)) shouldBe distance
    Values.decode(Values.encodeDistance(distance), DataType.Atomic(AtomicType.Distance)) shouldBe distance
  }

  it should "encode/decode durations" in {
    val duration = new Duration(3 * 60 * 1000)
    Values.decodeDuration(Values.encodeDuration(duration)) shouldBe duration
    Values.decode(Values.encodeDuration(duration)) shouldBe duration
    Values.decode(Values.encodeDuration(duration), DataType.Atomic(AtomicType.Duration)) shouldBe duration
  }

  it should "encode/decode locations" in {
    val location = LatLng.radians(0.7400196, -0.20961404)
    Values.decodeLocation(Values.encodeLocation(location)) shouldBe location
    Values.decode(Values.encodeLocation(location)) shouldBe location
    Values.decode(Values.encodeLocation(location), DataType.Atomic(AtomicType.Location)) shouldBe location
  }

  it should "encode/decode timestamps" in {
    val instant = new Instant(1388707109000L)
    Values.decodeTimestamp(Values.encodeTimestamp(instant)) shouldBe instant
    Values.decode(Values.encodeTimestamp(instant)) shouldBe instant
    Values.decode(Values.encodeTimestamp(instant), DataType.Atomic(AtomicType.Timestamp)) shouldBe instant
  }

  it should "encode/decode lists of integers" in {
    val list = Seq(1, 2, 3)
    val tpe = ListType(AtomicType.Integer)
    Values.decodeList(Values.encodeList(list, tpe).get, tpe) shouldBe list
    Values.decode(Values.encodeList(list, tpe).get) shouldBe list
  }

  it should "encode/decode maps of string=>integer" in {
    val map = Map("one" -> 1, "two" -> 2, "three" -> 3)
    val tpe = MapType(AtomicType.String, AtomicType.Integer)
    Values.decodeMap(Values.encodeMap(map, tpe).get, tpe) shouldBe map
    Values.decode(Values.encodeMap(map, tpe).get) shouldBe map
  }

  it should "encode/decode maps of string=>double" in {
    val map = Map("one" -> 1.1, "two" -> 2.2, "three" -> 3.3)
    val tpe = MapType(AtomicType.String, AtomicType.Double)
    Values.decodeMap(Values.encodeMap(map, tpe).get, tpe) shouldBe map
    Values.decode(Values.encodeMap(map, tpe).get) shouldBe map
  }

  it should "encode/decode maps of string=>location" in {
    val map = Map("one" -> LatLng.radians(0.7400196, -0.20961404), "two" -> LatLng.radians(1.0199704, 0.1832595), "three" -> LatLng.radians(-0.7155849, -0.1291543))
    val tpe = MapType(AtomicType.String, AtomicType.Location)
    Values.decodeMap(Values.encodeMap(map, tpe).get, tpe) shouldBe map
    Values.decode(Values.encodeMap(map, tpe).get) shouldBe map
  }

  it should "encode/decode maps of location=>string" in {
    val map = Map(LatLng.radians(0.7400196, -0.20961404) -> "one", LatLng.radians(1.0199704, 0.1832595) -> "two", LatLng.radians(-0.7155849, -0.1291543) -> "three")
    val tpe = MapType(AtomicType.Location, AtomicType.String)
    Values.decodeMap(Values.encodeMap(map, tpe).get, tpe) shouldBe map
    Values.decode(Values.encodeMap(map, tpe).get) shouldBe map
  }
}