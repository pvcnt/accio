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
class R1IntervalSpec extends UnitSpec {
  "R1Interval" should "be constructed" in {
    val unit = new R1Interval(0, 1)
    val negunit = new R1Interval(-1, 0)

    unit.lo shouldBe 0.0
    unit.hi shouldBe 1.0
    negunit.lo shouldBe -1.0
    negunit.hi shouldBe 0.0
  }

  it should "be constructed from two points" in {
    R1Interval.fromPointPair(4, 4) shouldBe new R1Interval(4, 4)
    R1Interval.fromPointPair(-1, -2) shouldBe new R1Interval(-2, -1)
    R1Interval.fromPointPair(-5, 3) shouldBe new R1Interval(-5, 3)
  }

  it should "tell if it is empty" in {
    val unit = new R1Interval(0, 1)
    val half = new R1Interval(0.5, 0.5)
    val empty = R1Interval.empty

    unit.isEmpty shouldBe false
    half.isEmpty shouldBe false
    empty.isEmpty shouldBe true
  }

  it should "return its center" in {
    val unit = new R1Interval(0, 1)
    val half = new R1Interval(0.5, 0.5)

    unit.getCenter shouldBe 0.5
    half.getCenter shouldBe 0.5
  }

  it should "return its length" in {
    val negunit = new R1Interval(-1, 0)
    val half = new R1Interval(0.5, 0.5)
    val empty = R1Interval.empty

    negunit.getLength shouldBe 1.0
    half.getLength shouldBe 0.0
    empty.getLength < 0 shouldBe true
  }

  it should "support double operators" in {
    val unit = new R1Interval(0, 1)
    unit.contains(0.5) shouldBe true
    unit.interiorContains(0.5) shouldBe true
    unit.contains(0) shouldBe true
    unit.interiorContains(0) shouldBe false
    unit.contains(1) shouldBe true
    unit.interiorContains(1) shouldBe false
  }

  it should "support interval operators" in {
    val unit = new R1Interval(0, 1)
    val negunit = new R1Interval(-1, 0)
    val half = new R1Interval(0.5, 0.5)
    val empty = R1Interval.empty

    testIntervalOps(empty, empty, "TTFF")
    testIntervalOps(empty, unit, "FFFF")
    testIntervalOps(unit, half, "TTTT")
    testIntervalOps(unit, unit, "TFTT")
    testIntervalOps(unit, empty, "TTFF")
    testIntervalOps(unit, negunit, "FFTF")
    testIntervalOps(unit, new R1Interval(0, 0.5), "TFTT")
    testIntervalOps(half, new R1Interval(0, 0.5), "FFTF")
  }

  it should "support adding new points" in {
    var r = R1Interval.empty.addPoint(5)
    r.lo() shouldBe 5.0
    r.hi() shouldBe 5.0

    r = r.addPoint(-1)
    r.lo() shouldBe -1.0
    r.hi() shouldBe 5.0

    r = r.addPoint(0)
    r.lo shouldBe -1.0
    r.hi shouldBe 5.0
  }

  it should "support expansion" in {
    val unit = new R1Interval(0, 1)
    val empty = R1Interval.empty
    empty.expanded(0.45) shouldBe empty
    unit.expanded(0.5) shouldBe new R1Interval(-0.5, 1.5)
  }

  it should "support union" in {
    val unit = new R1Interval(0, 1)
    val negunit = new R1Interval(-1, 0)
    val half = new R1Interval(0.5, 0.5)
    val empty = R1Interval.empty

    new R1Interval(99, 100).union(empty) shouldBe new R1Interval(99, 100)
    empty.union(new R1Interval(99, 100)) shouldBe new R1Interval(99, 100)
    new R1Interval(5, 3).union(new R1Interval(0, -2)).isEmpty shouldBe true
    new R1Interval(0, -2).union(new R1Interval(5, 3)).isEmpty shouldBe true
    unit.union(unit) shouldBe unit
    unit.union(negunit) shouldBe new R1Interval(-1, 1)
    negunit.union(unit) shouldBe new R1Interval(-1, 1)
    half.union(unit) shouldBe unit
  }

  it should "support intersection" in {
    val unit = new R1Interval(0, 1)
    val negunit = new R1Interval(-1, 0)
    val half = new R1Interval(0.5, 0.5)
    val empty = R1Interval.empty

    unit.intersection(half) shouldBe half
    unit.intersection(negunit) shouldBe new R1Interval(0, 0)
    negunit.intersection(half).isEmpty shouldBe true
    unit.intersection(empty).isEmpty shouldBe true
    empty.intersection(unit).isEmpty shouldBe true
  }

  /**
   * Test all of the interval operations on the given pair of intervals.
   * "expected_relation" is a sequence of "T" and "F" characters corresponding
   * to the expected results of contains(), interiorContains(), Intersects(),
   * and InteriorIntersects() respectively.
   */
  private def testIntervalOps(x: R1Interval, y: R1Interval, expectedRelation: String) = {
    x.contains(y) shouldBe (expectedRelation(0) == 'T')
    x.interiorContains(y) shouldBe (expectedRelation(1) == 'T')
    x.intersects(y) shouldBe (expectedRelation(2) == 'T')
    x.interiorIntersects(y) shouldBe (expectedRelation(3) == 'T')

    x.contains(y) shouldBe (x.union(y) == x)
    x.intersects(y) shouldBe !x.intersection(y).isEmpty
  }
}
