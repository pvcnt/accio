/*
 * Copyright 2005 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.common.geometry

import com.google.common.geometry.testing.GeometryHelpers
import fr.cnrs.liris.testing.UnitSpec

import scala.annotation.strictfp

@strictfp
class S2LatLngSpec extends UnitSpec with GeometryHelpers {
  val eps = 1e-9

  "S2LatLng" should "be constructed" in {
    val llRad = S2LatLng.fromRadians(S2.M_PI_4, S2.M_PI_2)
    llRad.lat.radians shouldBe S2.M_PI_4
    llRad.lng.radians shouldBe S2.M_PI_2
    llRad.isValid shouldBe true
    val llDeg = S2LatLng.fromDegrees(45, 90)
    llDeg shouldBe llRad
    llDeg.isValid shouldBe true
  }

  it should "be constructed from e5 representation" in {
    val test = S2LatLng.fromE5(123456, 98765)
    test.lat.degrees shouldBe (1.23456 +- eps)
    test.lng.degrees shouldBe (0.98765 +- eps)
  }

  it should "tell if it is valid" in {
    S2LatLng.fromDegrees(-91, 0).isValid shouldBe false
    S2LatLng.fromDegrees(0, 181).isValid shouldBe false
  }

  it should "support normalization" in {
    var bad = S2LatLng.fromDegrees(120, 200)
    bad.isValid shouldBe false
    var better = bad.normalized
    better.isValid shouldBe true
    better.lat shouldBe S1Angle.degrees(90)
    better.lng.radians shouldBe (S1Angle.degrees(-160).radians +- eps)

    bad = S2LatLng.fromDegrees(-100, -360)
    bad.isValid shouldBe false
    better = bad.normalized
    better.isValid shouldBe true
    better.lat shouldBe S1Angle.degrees(-90)
    better.lng.radians shouldBe (0d +- eps)
  }

  it should "support approximate equality" in {
    S2LatLng.fromDegrees(10, 20).add(S2LatLng.fromDegrees(20, 30))
        .approxEquals(S2LatLng.fromDegrees(30, 50)) shouldBe true
    S2LatLng.fromDegrees(10, 20).sub(S2LatLng.fromDegrees(20, 30))
        .approxEquals(S2LatLng.fromDegrees(-10, -10)) shouldBe true
    S2LatLng.fromDegrees(10, 20).mul(0.5).approxEquals(S2LatLng.fromDegrees(5, 10)) shouldBe true
  }

  it should "support conversion to point" in {
    // Test special cases: poles, "date line"
    new S2LatLng(S2LatLng.fromDegrees(90.0, 65.0).toPoint).lat.degrees shouldBe (90.0 +- eps)
    new S2LatLng(S2LatLng.fromRadians(-S2.M_PI_2, 1).toPoint).lat.radians shouldBe -S2.M_PI_2
    math.abs(new S2LatLng(S2LatLng.fromDegrees(12.2, 180.0).toPoint).lng.degrees) shouldBe (180.0 +- eps)
    math.abs(new S2LatLng(S2LatLng.fromRadians(0.1, -S2.M_PI).toPoint).lng.radians) shouldBe S2.M_PI

    // Test a bunch of random points.
    for (i <- 0 until 100000) {
      val p = randomPoint
      S2.approxEquals(p, new S2LatLng(p).toPoint) shouldBe true
    }
  }

  it should "compute distance with another LatLng" in {
    S2LatLng.fromDegrees(90, 0).getDistance(S2LatLng.fromDegrees(90, 0)).radians shouldBe 0.0
    S2LatLng.fromDegrees(-37, 25).getDistance(S2LatLng.fromDegrees(-66, -155)).degrees shouldBe (77d +- 1e-13)
    S2LatLng.fromDegrees(0, 165).getDistance(S2LatLng.fromDegrees(0, -80)).degrees shouldBe (115d +- 1e-13)
    S2LatLng.fromDegrees(47, -127).getDistance(S2LatLng.fromDegrees(-47, 53)).degrees shouldBe (180d +- 2e-6)
  }
}
