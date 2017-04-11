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

package fr.cnrs.liris.accio.core.api

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[References]].
 */
class ReferencesSpec extends UnitSpec {
  behavior of "References"

  it should "parse references" in {
    References.parse("foo/bar") shouldBe thrift.Reference("foo", "bar")
  }

  it should "return a parsable reference string representation" in {
    References.parse(Utils.toString(thrift.Reference("foo", "bar"))) shouldBe thrift.Reference("foo", "bar")
  }

  it should "detect non-parsable reference strings" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      References.parse("foo")
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      References.parse("foo/bar/baz")
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      References.parse("foo/")
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      References.parse("/foo")
    }
  }
}