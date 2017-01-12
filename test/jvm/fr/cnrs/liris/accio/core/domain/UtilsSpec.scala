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

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[Utils]].
 */
class UtilsSpec extends UnitSpec {
  behavior of "Utils"

  it should "parse data types" in {
    Utils.parseDataType("byte") shouldBe DataType(AtomicType.Byte)
    Utils.parseDataType("integer") shouldBe DataType(AtomicType.Integer)
    Utils.parseDataType("long") shouldBe DataType(AtomicType.Long)
    Utils.parseDataType("double") shouldBe DataType(AtomicType.Double)
    Utils.parseDataType("string") shouldBe DataType(AtomicType.String)
    Utils.parseDataType("boolean") shouldBe DataType(AtomicType.Boolean)
    Utils.parseDataType("location") shouldBe DataType(AtomicType.Location)
    Utils.parseDataType("timestamp") shouldBe DataType(AtomicType.Timestamp)
    Utils.parseDataType("distance") shouldBe DataType(AtomicType.Distance)
    Utils.parseDataType("duration") shouldBe DataType(AtomicType.Duration)
    Utils.parseDataType("dataset") shouldBe DataType(AtomicType.Dataset)
    Utils.parseDataType("list(integer)") shouldBe DataType(AtomicType.List, Seq(AtomicType.Integer))
    Utils.parseDataType("list (integer)") shouldBe DataType(AtomicType.List, Seq(AtomicType.Integer))
    Utils.parseDataType("list  (integer)") shouldBe DataType(AtomicType.List, Seq(AtomicType.Integer))
    Utils.parseDataType("list( integer)") shouldBe DataType(AtomicType.List, Seq(AtomicType.Integer))
    Utils.parseDataType("list(  integer)") shouldBe DataType(AtomicType.List, Seq(AtomicType.Integer))
    Utils.parseDataType("list(integer )") shouldBe DataType(AtomicType.List, Seq(AtomicType.Integer))
    Utils.parseDataType("list(integer  )") shouldBe DataType(AtomicType.List, Seq(AtomicType.Integer))
    Utils.parseDataType("set(string)") shouldBe DataType(AtomicType.Set, Seq(AtomicType.String))
    Utils.parseDataType("set (string)") shouldBe DataType(AtomicType.Set, Seq(AtomicType.String))
    Utils.parseDataType("set  (string)") shouldBe DataType(AtomicType.Set, Seq(AtomicType.String))
    Utils.parseDataType("set( string)") shouldBe DataType(AtomicType.Set, Seq(AtomicType.String))
    Utils.parseDataType("set(  string)") shouldBe DataType(AtomicType.Set, Seq(AtomicType.String))
    Utils.parseDataType("set(string )") shouldBe DataType(AtomicType.Set, Seq(AtomicType.String))
    Utils.parseDataType("set(string  )") shouldBe DataType(AtomicType.Set, Seq(AtomicType.String))
    Utils.parseDataType("map(location,double)") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
    Utils.parseDataType("map (location,double)") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
    Utils.parseDataType("map  (location,double)") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
    Utils.parseDataType("map( location,double)") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
    Utils.parseDataType("map(  location,double)") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
    Utils.parseDataType("map(location, double)") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
    Utils.parseDataType("map(location,  double)") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
    Utils.parseDataType("map(location,double )") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
    Utils.parseDataType("map(location,double  )") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
  }

  it should "return a parsable data type string representation" in {
    Utils.parseDataType(Utils.toString(DataType(AtomicType.Byte))) shouldBe DataType(AtomicType.Byte)
    Utils.parseDataType(Utils.toString(DataType(AtomicType.Integer))) shouldBe DataType(AtomicType.Integer)
    Utils.parseDataType(Utils.toString(DataType(AtomicType.Long))) shouldBe DataType(AtomicType.Long)
    Utils.parseDataType(Utils.toString(DataType(AtomicType.Double))) shouldBe DataType(AtomicType.Double)
    Utils.parseDataType(Utils.toString(DataType(AtomicType.String))) shouldBe DataType(AtomicType.String)
    Utils.parseDataType(Utils.toString(DataType(AtomicType.Boolean))) shouldBe DataType(AtomicType.Boolean)
    Utils.parseDataType(Utils.toString(DataType(AtomicType.Location))) shouldBe DataType(AtomicType.Location)
    Utils.parseDataType(Utils.toString(DataType(AtomicType.Timestamp))) shouldBe DataType(AtomicType.Timestamp)
    Utils.parseDataType(Utils.toString(DataType(AtomicType.Distance))) shouldBe DataType(AtomicType.Distance)
    Utils.parseDataType(Utils.toString(DataType(AtomicType.Duration))) shouldBe DataType(AtomicType.Duration)
    Utils.parseDataType(Utils.toString(DataType(AtomicType.Dataset))) shouldBe DataType(AtomicType.Dataset)
    Utils.parseDataType(Utils.toString(DataType(AtomicType.List, Seq(AtomicType.Distance)))) shouldBe DataType(AtomicType.List, Seq(AtomicType.Distance))
    Utils.parseDataType(Utils.toString(DataType(AtomicType.Set, Seq(AtomicType.Timestamp)))) shouldBe DataType(AtomicType.Set, Seq(AtomicType.Timestamp))
    Utils.parseDataType(Utils.toString(DataType(AtomicType.Map, Seq(AtomicType.Duration, AtomicType.String)))) shouldBe DataType(AtomicType.Map, Seq(AtomicType.Duration, AtomicType.String))
  }

  it should "detect non-parsable data type strings" in {
    val expected = intercept[IllegalArgumentException] {
      Utils.parseDataType("foo")
    }
    expected.getMessage shouldBe "Invalid data type: foo"
  }

  it should "parse references" in {
    Utils.parseReference("foo/bar") shouldBe Reference("foo", "bar")
  }

  it should "return a parsable reference string representation" in {
    Utils.parseReference(Utils.toString(Reference("foo", "bar"))) shouldBe Reference("foo", "bar")
  }

  it should "detect non-parsable reference strings" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      Utils.parseReference("foo")
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      Utils.parseReference("foo/bar/baz")
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      Utils.parseReference("foo/")
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      Utils.parseReference("/foo")
    }
  }
}