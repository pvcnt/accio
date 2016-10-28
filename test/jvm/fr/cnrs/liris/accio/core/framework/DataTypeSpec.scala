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

package fr.cnrs.liris.accio.core.framework

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[DataType]].
 */
class DataTypeSpec extends UnitSpec {
  behavior of "DataType"

  it should "parse simple types" in {
    DataType.parse("byte") shouldBe DataType.Byte
    DataType.parse("short") shouldBe DataType.Short
    DataType.parse("integer") shouldBe DataType.Integer
    DataType.parse("long") shouldBe DataType.Long
    DataType.parse("double") shouldBe DataType.Double
    DataType.parse("string") shouldBe DataType.String
    DataType.parse("boolean") shouldBe DataType.Boolean
    DataType.parse("location") shouldBe DataType.Location
    DataType.parse("timestamp") shouldBe DataType.Timestamp
    DataType.parse("distance") shouldBe DataType.Distance
    DataType.parse("duration") shouldBe DataType.Duration
    DataType.parse("dataset") shouldBe DataType.Dataset
    DataType.parse("image") shouldBe DataType.Image
    DataType.parse("list(integer)") shouldBe DataType.List(DataType.Integer)
    DataType.parse("list (integer)") shouldBe DataType.List(DataType.Integer)
    DataType.parse("list  (integer)") shouldBe DataType.List(DataType.Integer)
    DataType.parse("list( integer)") shouldBe DataType.List(DataType.Integer)
    DataType.parse("list(  integer)") shouldBe DataType.List(DataType.Integer)
    DataType.parse("list(integer )") shouldBe DataType.List(DataType.Integer)
    DataType.parse("list(integer  )") shouldBe DataType.List(DataType.Integer)
    DataType.parse("set(string)") shouldBe DataType.Set(DataType.String)
    DataType.parse("set (string)") shouldBe DataType.Set(DataType.String)
    DataType.parse("set  (string)") shouldBe DataType.Set(DataType.String)
    DataType.parse("set( string)") shouldBe DataType.Set(DataType.String)
    DataType.parse("set(  string)") shouldBe DataType.Set(DataType.String)
    DataType.parse("set(string )") shouldBe DataType.Set(DataType.String)
    DataType.parse("set(string  )") shouldBe DataType.Set(DataType.String)
    DataType.parse("map(location,double)") shouldBe DataType.Map(DataType.Location, DataType.Double)
    DataType.parse("map (location,double)") shouldBe DataType.Map(DataType.Location, DataType.Double)
    DataType.parse("map  (location,double)") shouldBe DataType.Map(DataType.Location, DataType.Double)
    DataType.parse("map( location,double)") shouldBe DataType.Map(DataType.Location, DataType.Double)
    DataType.parse("map(  location,double)") shouldBe DataType.Map(DataType.Location, DataType.Double)
    DataType.parse("map(location, double)") shouldBe DataType.Map(DataType.Location, DataType.Double)
    DataType.parse("map(location,  double)") shouldBe DataType.Map(DataType.Location, DataType.Double)
    DataType.parse("map(location,double )") shouldBe DataType.Map(DataType.Location, DataType.Double)
    DataType.parse("map(location,double  )") shouldBe DataType.Map(DataType.Location, DataType.Double)
  }

  it should "parse nested types" in {
    DataType.parse("map(string,map(integer,location))") shouldBe DataType.Map(DataType.String, DataType.Map(DataType.Integer, DataType.Location))
  }

  it should "return a parsable string representation" in {
    DataType.parse(DataType.Byte.toString) shouldBe DataType.Byte
    DataType.parse(DataType.Short.toString) shouldBe DataType.Short
    DataType.parse(DataType.Integer.toString) shouldBe DataType.Integer
    DataType.parse(DataType.Long.toString) shouldBe DataType.Long
    DataType.parse(DataType.Double.toString) shouldBe DataType.Double
    DataType.parse(DataType.String.toString) shouldBe DataType.String
    DataType.parse(DataType.Boolean.toString) shouldBe DataType.Boolean
    DataType.parse(DataType.Location.toString) shouldBe DataType.Location
    DataType.parse(DataType.Timestamp.toString) shouldBe DataType.Timestamp
    DataType.parse(DataType.Distance.toString) shouldBe DataType.Distance
    DataType.parse(DataType.Duration.toString) shouldBe DataType.Duration
    DataType.parse(DataType.Image.toString) shouldBe DataType.Image
    DataType.parse(DataType.Dataset.toString) shouldBe DataType.Dataset
    DataType.parse(DataType.List(DataType.Distance).toString) shouldBe DataType.List(DataType.Distance)
    DataType.parse(DataType.Set(DataType.Timestamp).toString) shouldBe DataType.Set(DataType.Timestamp)
    DataType.parse(DataType.Map(DataType.Duration, DataType.String).toString) shouldBe DataType.Map(DataType.Duration, DataType.String)
  }

  it should "detect non-parsable strings" in {
    val expected = intercept[IllegalArgumentException] {
      DataType.parse("foo")
    }
    expected.getMessage shouldBe "Invalid data type: foo"
  }
}