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

package fr.cnrs.liris.lumos.domain

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[LabelSelector]].
 */
class LabelSelectorSpec extends UnitSpec {
  behavior of "LabelSelector"

  it should "parse label selectors" in {
    LabelSelector.parse("foo") shouldBe Right(LabelSelector.present("foo"))
    LabelSelector.parse("!foo") shouldBe Right(LabelSelector.absent("foo"))
    LabelSelector.parse("foo=bar") shouldBe Right(LabelSelector.in("foo", Set("bar")))
    LabelSelector.parse("foo!=bar") shouldBe Right(LabelSelector.notIn("foo", Set("bar")))
    LabelSelector.parse("foo in (foo, bar)") shouldBe Right(LabelSelector.in("foo", Set("foo", "bar")))
    LabelSelector.parse("foo notin (foo, bar)") shouldBe Right(LabelSelector.notIn("foo", Set("foo", "bar")))
    LabelSelector.parse("foo,foo != bar") shouldBe Right(LabelSelector.present("foo") + LabelSelector.notIn("foo", Set("bar")))

    LabelSelector.parse("foo bar") shouldBe Left("'COMMA' expected but IDENTIFIER(bar) found")
  }

  it should "combine selectors" in {
    LabelSelector(LabelSelector.Req("foo", LabelSelector.Absent)) + LabelSelector(LabelSelector.Req("bar", LabelSelector.Present)) shouldBe LabelSelector(
      LabelSelector.Req("foo", LabelSelector.Absent), LabelSelector.Req("bar", LabelSelector.Present))
  }

  it should "reject an invalid requirement" in {
    var e = intercept[IllegalArgumentException] {
      LabelSelector(LabelSelector.Req("foo", LabelSelector.In, Set.empty))
    }
    e.getMessage shouldBe "requirement failed: 'In' operator requires at least one value"

    e = intercept[IllegalArgumentException] {
      LabelSelector(LabelSelector.Req("foo", LabelSelector.NotIn, Set.empty))
    }
    e.getMessage shouldBe "requirement failed: 'NotIn' operator requires at least one value"
  }
}
