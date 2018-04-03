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
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[DataTypes]].
 */
class DataTypesSpec extends UnitSpec {
  behavior of "DataTypes"

  it should "parse data types" in {
    DataTypes.parse("byte") shouldBe DataType(AtomicType.Byte)
    DataTypes.parse("integer") shouldBe DataType(AtomicType.Integer)
    DataTypes.parse("long") shouldBe DataType(AtomicType.Long)
    DataTypes.parse("double") shouldBe DataType(AtomicType.Double)
    DataTypes.parse("string") shouldBe DataType(AtomicType.String)
    DataTypes.parse("boolean") shouldBe DataType(AtomicType.Boolean)
    DataTypes.parse("location") shouldBe DataType(AtomicType.Location)
    DataTypes.parse("timestamp") shouldBe DataType(AtomicType.Timestamp)
    DataTypes.parse("distance") shouldBe DataType(AtomicType.Distance)
    DataTypes.parse("duration") shouldBe DataType(AtomicType.Duration)
    DataTypes.parse("dataset") shouldBe DataType(AtomicType.Dataset)
    DataTypes.parse("list(integer)") shouldBe DataType(AtomicType.List, Seq(AtomicType.Integer))
    DataTypes.parse("list (integer)") shouldBe DataType(AtomicType.List, Seq(AtomicType.Integer))
    DataTypes.parse("list  (integer)") shouldBe DataType(AtomicType.List, Seq(AtomicType.Integer))
    DataTypes.parse("list( integer)") shouldBe DataType(AtomicType.List, Seq(AtomicType.Integer))
    DataTypes.parse("list(  integer)") shouldBe DataType(AtomicType.List, Seq(AtomicType.Integer))
    DataTypes.parse("list(integer )") shouldBe DataType(AtomicType.List, Seq(AtomicType.Integer))
    DataTypes.parse("list(integer  )") shouldBe DataType(AtomicType.List, Seq(AtomicType.Integer))
    DataTypes.parse("set(string)") shouldBe DataType(AtomicType.Set, Seq(AtomicType.String))
    DataTypes.parse("set (string)") shouldBe DataType(AtomicType.Set, Seq(AtomicType.String))
    DataTypes.parse("set  (string)") shouldBe DataType(AtomicType.Set, Seq(AtomicType.String))
    DataTypes.parse("set( string)") shouldBe DataType(AtomicType.Set, Seq(AtomicType.String))
    DataTypes.parse("set(  string)") shouldBe DataType(AtomicType.Set, Seq(AtomicType.String))
    DataTypes.parse("set(string )") shouldBe DataType(AtomicType.Set, Seq(AtomicType.String))
    DataTypes.parse("set(string  )") shouldBe DataType(AtomicType.Set, Seq(AtomicType.String))
    DataTypes.parse("map(location,double)") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
    DataTypes.parse("map (location,double)") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
    DataTypes.parse("map  (location,double)") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
    DataTypes.parse("map( location,double)") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
    DataTypes.parse("map(  location,double)") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
    DataTypes.parse("map(location, double)") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
    DataTypes.parse("map(location,  double)") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
    DataTypes.parse("map(location,double )") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
    DataTypes.parse("map(location,double  )") shouldBe DataType(AtomicType.Map, Seq(AtomicType.Location, AtomicType.Double))
  }

  it should "return a parsable data type string representation" in {
    DataTypes.parse(DataTypes.stringify(DataType(AtomicType.Byte))) shouldBe DataType(AtomicType.Byte)
    DataTypes.parse(DataTypes.stringify(DataType(AtomicType.Integer))) shouldBe DataType(AtomicType.Integer)
    DataTypes.parse(DataTypes.stringify(DataType(AtomicType.Long))) shouldBe DataType(AtomicType.Long)
    DataTypes.parse(DataTypes.stringify(DataType(AtomicType.Double))) shouldBe DataType(AtomicType.Double)
    DataTypes.parse(DataTypes.stringify(DataType(AtomicType.String))) shouldBe DataType(AtomicType.String)
    DataTypes.parse(DataTypes.stringify(DataType(AtomicType.Boolean))) shouldBe DataType(AtomicType.Boolean)
    DataTypes.parse(DataTypes.stringify(DataType(AtomicType.Location))) shouldBe DataType(AtomicType.Location)
    DataTypes.parse(DataTypes.stringify(DataType(AtomicType.Timestamp))) shouldBe DataType(AtomicType.Timestamp)
    DataTypes.parse(DataTypes.stringify(DataType(AtomicType.Distance))) shouldBe DataType(AtomicType.Distance)
    DataTypes.parse(DataTypes.stringify(DataType(AtomicType.Duration))) shouldBe DataType(AtomicType.Duration)
    DataTypes.parse(DataTypes.stringify(DataType(AtomicType.Dataset))) shouldBe DataType(AtomicType.Dataset)
    DataTypes.parse(DataTypes.stringify(DataType(AtomicType.List, Seq(AtomicType.Distance)))) shouldBe DataType(AtomicType.List, Seq(AtomicType.Distance))
    DataTypes.parse(DataTypes.stringify(DataType(AtomicType.Set, Seq(AtomicType.Timestamp)))) shouldBe DataType(AtomicType.Set, Seq(AtomicType.Timestamp))
    DataTypes.parse(DataTypes.stringify(DataType(AtomicType.Map, Seq(AtomicType.Duration, AtomicType.String)))) shouldBe DataType(AtomicType.Map, Seq(AtomicType.Duration, AtomicType.String))
  }
}