/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

import fr.cnrs.liris.accio.core.domain.{ArgConstraint, ArgDef}
import fr.cnrs.liris.testing.UnitSpec
import fr.cnrs.liris.dal.core.api.{AtomicType, DataType, Value, Values}

/**
 * Unit tests for [[ValueValidator]].
 */
class ValueValidatorSpec extends UnitSpec {
  private val validator = new ValueValidator

  behavior of "ValueValidator"

  it should "detect an invalid data type" in {
    assertMessage(
      Values.encodeInteger(42),
      ArgDef("foo", DataType(AtomicType.String)),
      "Value of type integer does not match argument of type string")
  }

  it should "validate integers" in {
    assertValid(
      Values.encodeInteger(42),
      ArgDef("foo", DataType(AtomicType.Integer)))
    assertValid(
      Values.encodeInteger(42),
      ArgDef("foo", DataType(AtomicType.Integer), constraint = Some(ArgConstraint(minValue = Some(10), minInclusive = Some(true)))))
    assertValid(
      Values.encodeInteger(42),
      ArgDef("foo", DataType(AtomicType.Integer), constraint = Some(ArgConstraint(minValue = Some(42), minInclusive = Some(true)))))
    assertValid(
      Values.encodeInteger(42),
      ArgDef("foo", DataType(AtomicType.Integer), constraint = Some(ArgConstraint(maxValue = Some(50), maxInclusive = Some(true)))))
    assertValid(
      Values.encodeInteger(42),
      ArgDef("foo", DataType(AtomicType.Integer), constraint = Some(ArgConstraint(maxValue = Some(42), maxInclusive = Some(true)))))
    assertValid(
      Values.encodeInteger(42),
      ArgDef("foo", DataType(AtomicType.Integer), constraint = Some(ArgConstraint(minValue = Some(10), minInclusive = Some(true), maxValue = Some(50), maxInclusive = Some(true)))))
  }

  it should "validate bytes" in {
    assertValid(
      Values.encodeByte(42),
      ArgDef("foo", DataType(AtomicType.Byte)))
    assertValid(
      Values.encodeByte(42),
      ArgDef("foo", DataType(AtomicType.Byte), constraint = Some(ArgConstraint(minValue = Some(10), minInclusive = Some(true)))))
    assertValid(
      Values.encodeByte(42),
      ArgDef("foo", DataType(AtomicType.Byte), constraint = Some(ArgConstraint(minValue = Some(42), minInclusive = Some(true)))))
    assertValid(
      Values.encodeByte(42),
      ArgDef("foo", DataType(AtomicType.Byte), constraint = Some(ArgConstraint(maxValue = Some(50), maxInclusive = Some(true)))))
    assertValid(
      Values.encodeByte(42),
      ArgDef("foo", DataType(AtomicType.Byte), constraint = Some(ArgConstraint(maxValue = Some(42), maxInclusive = Some(true)))))
    assertValid(
      Values.encodeByte(42),
      ArgDef("foo", DataType(AtomicType.Byte), constraint = Some(ArgConstraint(minValue = Some(10), minInclusive = Some(true), maxValue = Some(50), maxInclusive = Some(true)))))
  }

  it should "validate longs" in {
    assertValid(
      Values.encodeLong(4200000000L),
      ArgDef("foo", DataType(AtomicType.Long)))
    assertValid(
      Values.encodeLong(4200000000L),
      ArgDef("foo", DataType(AtomicType.Long), constraint = Some(ArgConstraint(minValue = Some(1000000000L), minInclusive = Some(true)))))
    assertValid(
      Values.encodeLong(4200000000L),
      ArgDef("foo", DataType(AtomicType.Long), constraint = Some(ArgConstraint(minValue = Some(4200000000L), minInclusive = Some(true)))))
    assertValid(
      Values.encodeLong(4200000000L),
      ArgDef("foo", DataType(AtomicType.Long), constraint = Some(ArgConstraint(maxValue = Some(5000000000L), maxInclusive = Some(true)))))
    assertValid(
      Values.encodeLong(4200000000L),
      ArgDef("foo", DataType(AtomicType.Long), constraint = Some(ArgConstraint(maxValue = Some(4200000000L), maxInclusive = Some(true)))))
    assertValid(
      Values.encodeLong(4200000000L),
      ArgDef("foo", DataType(AtomicType.Long), constraint = Some(ArgConstraint(minValue = Some(1000000000L), minInclusive = Some(true), maxValue = Some(5000000000L), maxInclusive = Some(true)))))
  }

  it should "validate doubles" in {
    assertValid(
      Values.encodeDouble(42),
      ArgDef("foo", DataType(AtomicType.Double)))
    assertValid(
      Values.encodeDouble(42),
      ArgDef("foo", DataType(AtomicType.Double), constraint = Some(ArgConstraint(minValue = Some(10), minInclusive = Some(true)))))
    assertValid(
      Values.encodeDouble(42),
      ArgDef("foo", DataType(AtomicType.Double), constraint = Some(ArgConstraint(minValue = Some(42), minInclusive = Some(true)))))
    assertValid(
      Values.encodeDouble(42),
      ArgDef("foo", DataType(AtomicType.Double), constraint = Some(ArgConstraint(maxValue = Some(50), maxInclusive = Some(true)))))
    assertValid(
      Values.encodeDouble(42),
      ArgDef("foo", DataType(AtomicType.Double), constraint = Some(ArgConstraint(maxValue = Some(42), maxInclusive = Some(true)))))
    assertValid(
      Values.encodeDouble(42),
      ArgDef("foo", DataType(AtomicType.Double), constraint = Some(ArgConstraint(minValue = Some(10), minInclusive = Some(true), maxValue = Some(50), maxInclusive = Some(true)))))
  }

  it should "validate strings" in {
    assertValid(
      Values.encodeString("string"),
      ArgDef("foo", DataType(AtomicType.String)))
    assertValid(
      Values.encodeString("string"),
      ArgDef("foo", DataType(AtomicType.String), constraint = Some(ArgConstraint(allowedValues = Some(Set("str", "string"))))))
  }

  it should "validate collections of strings" in {
    // Lists.
    assertValid(
      Values.encodeList(Seq("string", "str"), AtomicType.String),
      ArgDef("foo", DataType(AtomicType.List, Seq(AtomicType.String))))
    assertValid(
      Values.encodeList(Seq("string", "str"), AtomicType.String),
      ArgDef("foo", DataType(AtomicType.List, Seq(AtomicType.String)), constraint = Some(ArgConstraint(allowedValues = Some(Set("str", "string"))))))

    // Sets.
    assertValid(
      Values.encodeSet(Set("string", "str"), AtomicType.String),
      ArgDef("foo", DataType(AtomicType.Set, Seq(AtomicType.String))))
    assertValid(
      Values.encodeSet(Set("string", "str"), AtomicType.String),
      ArgDef("foo", DataType(AtomicType.Set, Seq(AtomicType.String)), constraint = Some(ArgConstraint(allowedValues = Some(Set("str", "string"))))))
  }

  it should "validate collections" in {
    // Lists.
    assertValid(
      Values.encodeList(Seq(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.List, Seq(AtomicType.Integer))))
    assertValid(
      Values.encodeList(Seq(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.List, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(minValue = Some(2), minInclusive = Some(true)))))
    assertValid(
      Values.encodeList(Seq(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.List, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(minValue = Some(3), minInclusive = Some(true)))))
    assertValid(
      Values.encodeList(Seq(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.List, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(maxValue = Some(5), maxInclusive = Some(true)))))
    assertValid(
      Values.encodeList(Seq(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.List, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(maxValue = Some(3), maxInclusive = Some(true)))))
    assertValid(
      Values.encodeList(Seq(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.List, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(minValue = Some(2), minInclusive = Some(true), maxValue = Some(5), maxInclusive = Some(true)))))

    // Sets.
    assertValid(
      Values.encodeSet(Set(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.Set, Seq(AtomicType.Integer))))
    assertValid(
      Values.encodeSet(Set(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.Set, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(minValue = Some(2), minInclusive = Some(true)))))
    assertValid(
      Values.encodeSet(Set(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.Set, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(minValue = Some(3), minInclusive = Some(true)))))
    assertValid(
      Values.encodeSet(Set(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.Set, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(maxValue = Some(5), maxInclusive = Some(true)))))
    assertValid(
      Values.encodeSet(Set(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.Set, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(maxValue = Some(3), maxInclusive = Some(true)))))
    assertValid(
      Values.encodeSet(Set(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.Set, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(minValue = Some(2), minInclusive = Some(true), maxValue = Some(5), maxInclusive = Some(true)))))
  }

  it should "detect invalid integers" in {
    assertMessage(
      Values.encodeInteger(5),
      ArgDef("foo", DataType(AtomicType.Integer), constraint = Some(ArgConstraint(minValue = Some(10), minInclusive = Some(true)))),
      "Value must be >= 10.0")
    assertMessage(
      Values.encodeInteger(42),
      ArgDef("foo", DataType(AtomicType.Integer), constraint = Some(ArgConstraint(minValue = Some(42), minInclusive = Some(false)))),
      "Value must be > 42.0")
    assertMessage(
      Values.encodeInteger(50),
      ArgDef("foo", DataType(AtomicType.Integer), constraint = Some(ArgConstraint(maxValue = Some(42), maxInclusive = Some(true)))),
      "Value must be <= 42.0")
    assertMessage(
      Values.encodeInteger(42),
      ArgDef("foo", DataType(AtomicType.Integer), constraint = Some(ArgConstraint(maxValue = Some(42), maxInclusive = Some(false)))),
      "Value must be < 42.0")
    assertMessage(
      Values.encodeInteger(5),
      ArgDef("foo", DataType(AtomicType.Integer), constraint = Some(ArgConstraint(minValue = Some(10), minInclusive = Some(true), maxValue = Some(42), maxInclusive = Some(true)))),
      "Value must be >= 10.0")
    assertMessage(
      Values.encodeInteger(50),
      ArgDef("foo", DataType(AtomicType.Integer), constraint = Some(ArgConstraint(minValue = Some(10), minInclusive = Some(true), maxValue = Some(42), maxInclusive = Some(true)))),
      "Value must be <= 42.0")
  }

  it should "detect invalid bytes" in {
    assertMessage(
      Values.encodeByte(5),
      ArgDef("foo", DataType(AtomicType.Byte), constraint = Some(ArgConstraint(minValue = Some(10), minInclusive = Some(true)))),
      "Value must be >= 10.0")
    assertMessage(
      Values.encodeByte(42),
      ArgDef("foo", DataType(AtomicType.Byte), constraint = Some(ArgConstraint(minValue = Some(42), minInclusive = Some(false)))),
      "Value must be > 42.0")
    assertMessage(
      Values.encodeByte(50),
      ArgDef("foo", DataType(AtomicType.Byte), constraint = Some(ArgConstraint(maxValue = Some(42), maxInclusive = Some(true)))),
      "Value must be <= 42.0")
    assertMessage(
      Values.encodeByte(42),
      ArgDef("foo", DataType(AtomicType.Byte), constraint = Some(ArgConstraint(maxValue = Some(42), maxInclusive = Some(false)))),
      "Value must be < 42.0")
    assertMessage(
      Values.encodeByte(5),
      ArgDef("foo", DataType(AtomicType.Byte), constraint = Some(ArgConstraint(minValue = Some(10), minInclusive = Some(true), maxValue = Some(42), maxInclusive = Some(true)))),
      "Value must be >= 10.0")
    assertMessage(
      Values.encodeByte(50),
      ArgDef("foo", DataType(AtomicType.Byte), constraint = Some(ArgConstraint(minValue = Some(10), minInclusive = Some(true), maxValue = Some(42), maxInclusive = Some(true)))),
      "Value must be <= 42.0")
  }

  it should "detect invalid longs" in {
    assertMessage(
      Values.encodeLong(500000000L),
      ArgDef("foo", DataType(AtomicType.Long), constraint = Some(ArgConstraint(minValue = Some(1000000000L), minInclusive = Some(true)))),
      "Value must be >= 1.0E9")
    assertMessage(
      Values.encodeLong(4200000000L),
      ArgDef("foo", DataType(AtomicType.Long), constraint = Some(ArgConstraint(minValue = Some(4200000000L), minInclusive = Some(false)))),
      "Value must be > 4.2E9")
    assertMessage(
      Values.encodeLong(5000000000L),
      ArgDef("foo", DataType(AtomicType.Long), constraint = Some(ArgConstraint(maxValue = Some(4200000000L), maxInclusive = Some(true)))),
      "Value must be <= 4.2E9")
    assertMessage(
      Values.encodeLong(4200000000L),
      ArgDef("foo", DataType(AtomicType.Long), constraint = Some(ArgConstraint(maxValue = Some(4200000000L), maxInclusive = Some(false)))),
      "Value must be < 4.2E9")
    assertMessage(
      Values.encodeLong(500000000L),
      ArgDef("foo", DataType(AtomicType.Long), constraint = Some(ArgConstraint(minValue = Some(1000000000L), minInclusive = Some(true), maxValue = Some(4200000000L), maxInclusive = Some(true)))),
      "Value must be >= 1.0E9")
    assertMessage(
      Values.encodeLong(5000000000L),
      ArgDef("foo", DataType(AtomicType.Long), constraint = Some(ArgConstraint(minValue = Some(1000000000L), minInclusive = Some(true), maxValue = Some(4200000000L), maxInclusive = Some(true)))),
      "Value must be <= 4.2E9")
  }

  it should "detect invalid doubles" in {
    assertMessage(
      Values.encodeDouble(5),
      ArgDef("foo", DataType(AtomicType.Double), constraint = Some(ArgConstraint(minValue = Some(10), minInclusive = Some(true)))),
      "Value must be >= 10.0")
    assertMessage(
      Values.encodeDouble(42),
      ArgDef("foo", DataType(AtomicType.Double), constraint = Some(ArgConstraint(minValue = Some(42), minInclusive = Some(false)))),
      "Value must be > 42.0")
    assertMessage(
      Values.encodeDouble(50),
      ArgDef("foo", DataType(AtomicType.Double), constraint = Some(ArgConstraint(maxValue = Some(42), maxInclusive = Some(true)))),
      "Value must be <= 42.0")
    assertMessage(
      Values.encodeDouble(42),
      ArgDef("foo", DataType(AtomicType.Double), constraint = Some(ArgConstraint(maxValue = Some(42), maxInclusive = Some(false)))),
      "Value must be < 42.0")
    assertMessage(
      Values.encodeDouble(5),
      ArgDef("foo", DataType(AtomicType.Double), constraint = Some(ArgConstraint(minValue = Some(10), minInclusive = Some(true), maxValue = Some(42), maxInclusive = Some(true)))),
      "Value must be >= 10.0")
    assertMessage(
      Values.encodeDouble(50),
      ArgDef("foo", DataType(AtomicType.Double), constraint = Some(ArgConstraint(minValue = Some(10), minInclusive = Some(true), maxValue = Some(42), maxInclusive = Some(true)))),
      "Value must be <= 42.0")
  }

  it should "detect invalid strings" in {
    assertMessage(
      Values.encodeString("bar"),
      ArgDef("foo", DataType(AtomicType.String), constraint = Some(ArgConstraint(allowedValues = Some(Set("str", "string"))))),
      "Value must be one of {str,string}")
  }

  it should "detect invalid collections of strings" in {
    // Lists.
    assertMessage(
      Values.encodeList(Seq("string", "bar"), AtomicType.String),
      ArgDef("foo", DataType(AtomicType.List, Seq(AtomicType.String)), constraint = Some(ArgConstraint(allowedValues = Some(Set("str", "string"))))),
      "Value must be one of {str,string}")

    // Sets.
    assertMessage(
      Values.encodeSet(Set("string", "bar"), AtomicType.String),
      ArgDef("foo", DataType(AtomicType.Set, Seq(AtomicType.String)), constraint = Some(ArgConstraint(allowedValues = Some(Set("str", "string"))))),
      "Value must be one of {str,string}")
  }

  it should "detect invalid collections" in {
    // Lists.
    assertMessage(
      Values.encodeList(Seq(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.List, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(minValue = Some(4), minInclusive = Some(true)))),
      "Value must be >= 4.0")
    assertMessage(
      Values.encodeList(Seq(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.List, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(minValue = Some(3), minInclusive = Some(false)))),
      "Value must be > 3.0")
    assertMessage(
      Values.encodeList(Seq(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.List, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(maxValue = Some(2), maxInclusive = Some(true)))),
      "Value must be <= 2.0")
    assertMessage(
      Values.encodeList(Seq(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.List, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(maxValue = Some(3), maxInclusive = Some(false)))),
      "Value must be < 3.0")
    assertMessage(
      Values.encodeList(Seq(1, 2), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.List, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(minValue = Some(3), minInclusive = Some(true), maxValue = Some(4), maxInclusive = Some(true)))),
      "Value must be >= 3.0")
    assertMessage(
      Values.encodeList(Seq(1, 2, 3, 4, 5), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.List, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(minValue = Some(3), minInclusive = Some(true), maxValue = Some(4), maxInclusive = Some(true)))),
      "Value must be <= 4.0")

    // Sets.
    assertMessage(
      Values.encodeSet(Set(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.Set, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(minValue = Some(4), minInclusive = Some(true)))),
      "Value must be >= 4.0")
    assertMessage(
      Values.encodeSet(Set(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.Set, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(minValue = Some(3), minInclusive = Some(false)))),
      "Value must be > 3.0")
    assertMessage(
      Values.encodeSet(Set(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.Set, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(maxValue = Some(2), maxInclusive = Some(true)))),
      "Value must be <= 2.0")
    assertMessage(
      Values.encodeSet(Set(1, 2, 3), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.Set, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(maxValue = Some(3), maxInclusive = Some(false)))),
      "Value must be < 3.0")
    assertMessage(
      Values.encodeSet(Set(1, 2), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.Set, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(minValue = Some(3), minInclusive = Some(true), maxValue = Some(4), maxInclusive = Some(true)))),
      "Value must be >= 3.0")
    assertMessage(
      Values.encodeSet(Set(1, 2, 3, 4, 5), AtomicType.Integer),
      ArgDef("foo", DataType(AtomicType.Set, Seq(AtomicType.Integer)), constraint = Some(ArgConstraint(minValue = Some(3), minInclusive = Some(true), maxValue = Some(4), maxInclusive = Some(true)))),
      "Value must be <= 4.0")
  }

  private def assertMessage(value: Value, argDef: ArgDef, messages: String*) = {
    validator.validate(value, argDef).map(_.message) should contain theSameElementsAs messages
  }

  private def assertValid(value: Value, argDef: ArgDef) = {
    validator.validate(value, argDef) should have size 0
  }
}
