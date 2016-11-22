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
 * Unit tests for [[Point]].
 */
class PointSpec extends UnitSpec {
  val eps = 1e-8
  behavior of "Point"

  it should "be created from coordinates" in {
    val point = Point(3.14, 42)
    point.x shouldBe 3.14
    point.y shouldBe 42
  }

  it should "compute correct distances with another point" in {
    val point1 = Point(3.14, 42)
    var point2 = Point(3.14, 50)
    point1.distance(point2).meters shouldBe closeTo(8, 1e-13)

    // Distance is commutative.
    point1.distance(point2) shouldBe point2.distance(point1)

    point2 = Point(4.14, 42)
    point1.distance(point2).meters shouldBe closeTo(1, 1e-13)

    point2 = Point(4.14, 50)
    point1.distance(point2).meters shouldBe closeTo(8.062257758, 1e-6)
  }

  it should "be convertable to coordinates" in {
    Point(3.14, 42).toSeq shouldBe Seq(3.14, 42)
  }

  it should "be convertable to a lat/lng" in {
    val point1 = Point(3.14, 42)
    val point2 = Point(3, 50)
    val latLng1 = point1.toLatLng
    val latLng2 = point2.toLatLng

    // Distance is consistent.
    latLng1.distance(latLng2).meters shouldBe closeTo(point1.distance(point2).meters, eps)

    // It can be converted back into the same point.
    latLng1.toPoint.x shouldBe closeTo(3.14, eps)
    latLng1.toPoint.y shouldBe closeTo(42, eps)
  }

  it should "support arithmetic operations" in {
    val point1 = Point(6, 42)
    val point2 = Point(2, 10)
    point1 + point2 shouldBe Point(8, 52)
    point1 - point2 shouldBe Point(4, 32)
    point2 * 2 shouldBe Point(4, 20)
    point2 / 2 shouldBe Point(1, 5)
  }
}