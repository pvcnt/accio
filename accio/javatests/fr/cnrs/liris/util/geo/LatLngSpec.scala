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

package fr.cnrs.liris.util.geo

import com.google.common.geometry.S2LatLng
import fr.cnrs.liris.testing.UnitSpec

import scala.util.Random

/**
 * Unit tests for [[LatLng]].
 */
class LatLngSpec extends UnitSpec {
  val eps = 1e-13
  behavior of "LatLng"

  it should "be created from degrees" in {
    val latLng = LatLng.degrees(50, -120)
    latLng.lat.degrees shouldBe closeTo(50, eps)
    latLng.lng.degrees shouldBe closeTo(-120, eps)
  }

  it should "be created from radians" in {
    val latLng = LatLng.radians(1.1, 2.6)
    latLng.lat.radians shouldBe closeTo(1.1, eps)
    latLng.lng.radians shouldBe closeTo(2.6, eps)
  }

  it should "compute correct distances with another lat/lng" in {
    (0 until 50).foreach { _ =>
      val lat1 = Random.nextInt(180) - 90
      val lat2 = Random.nextInt(180) - 90
      val lng1 = Random.nextInt(360) - 180
      val lng2 = Random.nextInt(360) - 180
      LatLng.degrees(lat1, lng1).distance(LatLng.degrees(lat2, lng2)).meters shouldBe
        closeTo(S2LatLng.fromDegrees(lat1, lng1).getEarthDistance(S2LatLng.fromDegrees(lat2, lng2)), eps)
    }

    // Distance is commutative.
    val latLng1 = LatLng.degrees(50, -120)
    val latLng2 = LatLng.degrees(48, -115)
    latLng1.distance(latLng2) shouldBe latLng2.distance(latLng1)
  }

  it should "be convertable to coordinates" in {
    val coords = LatLng.degrees(50, -120).toSeq
    coords should have size 2
    coords.last shouldBe closeTo(50, eps)
    coords.head shouldBe closeTo(-120, eps)
  }

  it should "be convertable to a point" in {
    val latLng1 = LatLng.degrees(50, -120)
    val latLng2 = LatLng.degrees(50.005, -120.005)
    val point1 = latLng1.toPoint
    val point2 = latLng2.toPoint

    // Distance is consistent.
    //point1.distance(point2).meters shouldBe closeTo(latLng1.distance(latLng2).meters, eps)

    // It can be converted back into the same point.
    point1.toLatLng.lat.degrees shouldBe closeTo(50, eps)
    point1.toLatLng.lng.degrees shouldBe closeTo(-120, eps)
  }

  it should "be parsable" in {
    val latLng = LatLng.parse("50,-120")
    latLng.lat.degrees shouldBe closeTo(50, eps)
    latLng.lng.degrees shouldBe closeTo(-120, eps)
  }

  it should "generate a parsable representation" in {
    LatLng.parse(LatLng.degrees(50, -120).toString) shouldBe LatLng.degrees(50, -120)
    LatLng.parse(LatLng.radians(1.1, 2.6).toString) shouldBe LatLng.radians(1.1, 2.6)
  }
}