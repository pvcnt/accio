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

import fr.cnrs.liris.testing.UnitSpec

import scala.annotation.strictfp

@strictfp
class S1AngleSpec extends UnitSpec {
  "S1Angle" should "convert between radians and degrees" in {
    // Check that the conversion between Pi radians and 180 degrees is exact.
    S1Angle.radians(Math.PI).radians shouldBe math.Pi
    S1Angle.radians(Math.PI).degrees shouldBe 180.0
    S1Angle.degrees(180).radians shouldBe math.Pi
    S1Angle.degrees(180).degrees shouldBe 180.0

    S1Angle.radians(Math.PI / 2).degrees shouldBe 90.0

    // Check negative angles.
    S1Angle.radians(-Math.PI / 2).degrees shouldBe -90.0
    S1Angle.degrees(-45).radians shouldBe -math.Pi / 4
  }

  it should "handle E5 representation" in {
    S1Angle.e5(2000000) shouldBe S1Angle.degrees(20)
    S1Angle.degrees(12.34567).e5 shouldBe 1234567
  }

  it should "handle E6 representation" in {
    S1Angle.e6(-60000000) shouldBe S1Angle.degrees(-60)
    S1Angle.degrees(12.345678).e6 shouldBe 12345678
  }

  it should "handle E7 representation" in {
    S1Angle.e7(750000000) shouldBe S1Angle.degrees(75)
    S1Angle.degrees(-12.3456789).e7 shouldBe -123456789
  }
}