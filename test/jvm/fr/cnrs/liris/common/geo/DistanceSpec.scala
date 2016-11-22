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

package fr.cnrs.liris.common.geo

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[Distance]].
 */
class DistanceSpec extends UnitSpec {
  behavior of "Distance"

  it should "be created from meters" in {
    val d = Distance.meters(3.14)
    d.isFinite shouldBe true
    d.isInfinite shouldBe false
    d.meters shouldBe 3.14
  }

  it should "be created from kilometers" in {
    val d = Distance.kilometers(3.14)
    d.isFinite shouldBe true
    d.isInfinite shouldBe false
    d.kilometers shouldBe 3.14
  }

  it should "be created from miles" in {
    val d = Distance.miles(3.14)
    d.isFinite shouldBe true
    d.isInfinite shouldBe false
    d.miles shouldBe 3.14
  }

  it should "be convertable from one unit to another" in {
    var d = Distance.meters(3140)
    d.kilometers shouldBe 3.14

    d = Distance.kilometers(3.14)
    d.meters shouldBe 3140
  }

  it should "be infinite if specified" in {
    var d = Distance.meters(Double.PositiveInfinity)
    d.isFinite shouldBe false
    d.isInfinite shouldBe true

    d = Distance.kilometers(Double.PositiveInfinity)
    d.isFinite shouldBe false
    d.isInfinite shouldBe true

    d = Distance.miles(Double.PositiveInfinity)
    d.isFinite shouldBe false
    d.isInfinite shouldBe true

    Distance.Infinity.isFinite shouldBe false
    Distance.Infinity.isInfinite shouldBe true
  }

  it should "be parsable" in {
    Distance.parse("3.14.meters") shouldBe Distance.meters(3.14)
    Distance.parse("314.meters") shouldBe Distance.meters(314)
    Distance.parse("3.14.kilometers") shouldBe Distance.kilometers(3.14)
    Distance.parse("314.kilometers") shouldBe Distance.kilometers(314)
    Distance.parse("3.14.miles") shouldBe Distance.miles(3.14)
    Distance.parse("314.miles") shouldBe Distance.miles(314)
  }

  it should "produce a parsable representation" in {
    Distance.parse(Distance.meters(3.14).toString) shouldBe Distance.meters(3.14)
    Distance.parse(Distance.meters(314).toString) shouldBe Distance.meters(314)
    Distance.parse(Distance.kilometers(3.14).toString) shouldBe Distance.kilometers(3.14)
    Distance.parse(Distance.kilometers(314).toString) shouldBe Distance.kilometers(314)
    Distance.parse(Distance.miles(3.14).toString) shouldBe Distance.miles(3.14)
    Distance.parse(Distance.miles(314).toString) shouldBe Distance.miles(314)
  }

  it should "support arithmetic operations" in {
    val d1 = Distance.meters(10)
    val d2 = Distance.meters(2)
    d1 / d2 shouldBe 5
    d1 + d2 shouldBe Distance.meters(12)
    d1 - d2 shouldBe Distance.meters(8)
    d1 / 2 shouldBe Distance.meters(5)
    d1 * 2 shouldBe Distance.meters(20)
  }

  it should "have an ordering" in {
    Distance.meters(10) shouldBe <(Distance.meters(11))
    Distance.meters(10) shouldBe <(Distance.kilometers(10))
    Distance.Infinity shouldBe >(Distance.meters(10000))
  }
}