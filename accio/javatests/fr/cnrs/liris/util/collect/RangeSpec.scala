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

package fr.cnrs.liris.util.collect

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[Range]].
 *
 * @see https://github.com/google/guava/blob/master/guava-tests/test/com/google/common/collect/RangeTest.java
 */
class RangeSpec extends UnitSpec {
  behavior of "Range"

  it should "work with an open range" in {
    val range = Range.open(4, 8)
    checkContains(range)
    range.hasLowerBound shouldBe true
    range.lowerEndpoint shouldBe 4
    range.lowerBoundType shouldBe Range.Open
    range.hasUpperBound shouldBe true
    range.upperEndpoint shouldBe 8
    range.upperBoundType shouldBe Range.Open
    range.isEmpty shouldBe false
    range.toString shouldBe "(4..8)"
  }

  it should "detect an invalid open range" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      Range.open(4, 3)
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      Range.open(3, 3)
    }
  }

  it should "work with a closed range" in {
    val range = Range.closed(5, 7)
    checkContains(range)
    range.hasLowerBound shouldBe true
    range.lowerEndpoint shouldBe 5
    range.lowerBoundType shouldBe Range.Closed
    range.hasUpperBound shouldBe true
    range.upperEndpoint shouldBe 7
    range.upperBoundType shouldBe Range.Closed
    range.isEmpty shouldBe false
    range.toString shouldBe "[5..7]"
  }

  it should "detect an invalid closed range" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      Range.closed(4, 3)
    }
  }

  it should "work with a open-closed range" in {
    val range = Range.openClosed(4, 7)
    checkContains(range)
    range.hasLowerBound shouldBe true
    range.lowerEndpoint shouldBe 4
    range.lowerBoundType shouldBe Range.Open
    range.hasUpperBound shouldBe true
    range.upperEndpoint shouldBe 7
    range.upperBoundType shouldBe Range.Closed
    range.isEmpty shouldBe false
    range.toString shouldBe "(4..7]"
  }

  it should "work with a closed-open range" in {
    val range = Range.closedOpen(5, 8)
    checkContains(range)
    range.hasLowerBound shouldBe true
    range.lowerEndpoint shouldBe 5
    range.lowerBoundType shouldBe Range.Closed
    range.hasUpperBound shouldBe true
    range.upperEndpoint shouldBe 8
    range.upperBoundType shouldBe Range.Open
    range.isEmpty shouldBe false
    range.toString shouldBe "[5..8)"
  }

  private def checkContains(range: Range[Int]): Unit = {
    range.contains(4) shouldBe false
    range.contains(5) shouldBe true
    range.contains(7) shouldBe true
    range.contains(8) shouldBe false
  }

  it should "work with a singleton" in {
    val range = Range.closed(4, 4)
    range.contains(3) shouldBe false
    range.contains(4) shouldBe true
    range.contains(5) shouldBe false
    range.hasLowerBound shouldBe true
    range.lowerEndpoint shouldBe 4
    range.lowerBoundType shouldBe Range.Closed
    range.hasUpperBound shouldBe true
    range.upperEndpoint shouldBe 4
    range.upperBoundType shouldBe Range.Closed
    range.isEmpty shouldBe false
    range.toString shouldBe "[4..4]"
  }

  it should "work with an empty range closed-open" in {
    val range = Range.closedOpen(4, 4)
    range.contains(3) shouldBe false
    range.contains(4) shouldBe false
    range.contains(5) shouldBe false
    range.hasLowerBound shouldBe true
    range.lowerEndpoint shouldBe 4
    range.lowerBoundType shouldBe Range.Closed
    range.hasUpperBound shouldBe true
    range.upperEndpoint shouldBe 4
    range.upperBoundType shouldBe Range.Open
    range.isEmpty shouldBe true
    range.toString shouldBe "[4..4)"
  }

  it should "work with an empty range open-closed" in {
    val range = Range.openClosed(4, 4)
    range.contains(3) shouldBe false
    range.contains(4) shouldBe false
    range.contains(5) shouldBe false
    range.hasLowerBound shouldBe true
    range.lowerEndpoint shouldBe 4
    range.lowerBoundType shouldBe Range.Open
    range.hasUpperBound shouldBe true
    range.upperEndpoint shouldBe 4
    range.upperBoundType shouldBe Range.Closed
    range.isEmpty shouldBe true
    range.toString shouldBe "(4..4]"
  }

  it should "work with a less-than range" in {
    val range = Range.lessThan(5)
    range.contains(Integer.MIN_VALUE) shouldBe true
    range.contains(4) shouldBe true
    range.contains(5) shouldBe false
    assertUnboundedBelow(range)
    range.hasUpperBound shouldBe true
    range.upperEndpoint shouldBe 5
    range.upperBoundType shouldBe Range.Open
    range.isEmpty shouldBe false
    range.toString shouldBe "(-\u221e..5)"
  }

  it should "work with a greater-than range" in {
    val range = Range.greaterThan(5)
    range.contains(5) shouldBe false
    range.contains(6) shouldBe true
    range.contains(Integer.MAX_VALUE) shouldBe true
    range.hasLowerBound shouldBe true
    range.lowerEndpoint shouldBe 5
    range.lowerBoundType shouldBe Range.Open
    assertUnboundedAbove(range)
    range.isEmpty shouldBe false
    range.toString shouldBe "(5..+\u221e)"
  }

  it should "work with a at-least range" in {
    val range = Range.atLeast(6)
    range.contains(5) shouldBe false
    range.contains(6) shouldBe true
    range.contains(Integer.MAX_VALUE) shouldBe true
    range.hasLowerBound shouldBe true
    range.lowerEndpoint shouldBe 6
    range.lowerBoundType shouldBe Range.Closed
    assertUnboundedAbove(range)
    range.isEmpty shouldBe false
    range.toString shouldBe "[6..+\u221e)"
  }

  it should "work with a at-most range" in {
    val range = Range.atMost(4)
    range.contains(Integer.MIN_VALUE) shouldBe true
    range.contains(4) shouldBe true
    range.contains(5) shouldBe false
    assertUnboundedBelow(range)
    range.hasUpperBound shouldBe true
    range.upperEndpoint shouldBe 4
    range.upperBoundType shouldBe Range.Closed
    range.isEmpty shouldBe false
    range.toString shouldBe "(-\u221e..4]"
  }

  it should "work with an all range" in {
    val range = Range.all[Int]
    range.contains(Int.MinValue) shouldBe true
    range.contains(Int.MaxValue) shouldBe true
    assertUnboundedBelow(range)
    assertUnboundedAbove(range)
    range.isEmpty shouldBe false
    range.toString shouldBe "(-\u221e..+\u221e)"
    Range.all shouldBe range
  }

  private def assertUnboundedBelow(range: Range[Int]): Unit = {
    range.hasLowerBound shouldBe false
    a[NoSuchElementException] shouldBe thrownBy {
      range.lowerEndpoint
    }
    /*a[NoSuchElementException] shouldBe thrownBy {
      range.lowerBoundType
    }*/
  }

  private def assertUnboundedAbove(range: Range[Int]): Unit = {
    range.hasUpperBound shouldBe false
    a[NoSuchElementException] shouldBe thrownBy {
      range.upperEndpoint
    }
    /*a[NoSuchElementException] shouldBe thrownBy {
      range.upperBoundType
    }*/
  }

  it should "check connection between two ranges" in {
    Range.closed(3, 5).isConnected(Range.open(5, 6)) shouldBe true
    Range.closed(3, 5).isConnected(Range.openClosed(5, 5)) shouldBe true
    Range.open(3, 5).isConnected(Range.closed(5, 6)) shouldBe true
    Range.closed(3, 7).isConnected(Range.open(6, 8)) shouldBe true
    Range.open(3, 7).isConnected(Range.closed(5, 6)) shouldBe true
    Range.closed(3, 5).isConnected(Range.closed(7, 8)) shouldBe false
    Range.closed(3, 5).isConnected(Range.closedOpen(7, 7)) shouldBe false
  }


  it should "detect empty intersection" in {
    val range = Range.closedOpen(3, 3)
    range.intersection(range) shouldBe range

    an[IllegalArgumentException] shouldBe thrownBy {
      range.intersection(Range.open(3, 5))
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      range.intersection(Range.closed(0, 2))
    }
  }

  it should "detect de-facto empty intersection" in {
    var range = Range.open(3, 4)
    range.intersection(range) shouldBe range

    range.intersection(Range.atMost(3)) shouldBe Range.openClosed(3, 3)
    range.intersection(Range.atLeast(4)) shouldBe Range.closedOpen(4, 4)

    an[IllegalArgumentException] shouldBe thrownBy {
      range.intersection(Range.lessThan(3))
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      range.intersection(Range.greaterThan(4))
    }

    range = Range.closed(3, 4)
    range.intersection(Range.greaterThan(4)) shouldBe Range.openClosed(4, 4)
  }

  it should "compute intersections with singletons" in {
    val range = Range.closed(3, 3)
    range.intersection(range) shouldBe range

    range.intersection(Range.atMost(4)) shouldBe range
    range.intersection(Range.atMost(3)) shouldBe range
    range.intersection(Range.atLeast(3)) shouldBe range
    range.intersection(Range.atLeast(2)) shouldBe range

    range.intersection(Range.lessThan(3)) shouldBe Range.closedOpen(3, 3)
    range.intersection(Range.greaterThan(3)) shouldBe Range.openClosed(3, 3)

    an[IllegalArgumentException] shouldBe thrownBy {
      range.intersection(Range.atLeast(4))
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      range.intersection(Range.atMost(2))
    }
  }

  it should "compute intersections" in {
    val range = Range.closed(4, 8)

    // separate below
    an[IllegalArgumentException] shouldBe thrownBy {
      range.intersection(Range.closed(0, 2))
    }

    // adjacent below
    range.intersection(Range.closedOpen(2, 4)) shouldBe Range.closedOpen(4, 4)

    // overlap below
    range.intersection(Range.closed(2, 6)) shouldBe Range.closed(4, 6)

    // enclosed with same start
    range.intersection(Range.closed(4, 6)) shouldBe Range.closed(4, 6)

    // enclosed, interior
    range.intersection(Range.closed(5, 7)) shouldBe Range.closed(5, 7)

    // enclosed with same end
    range.intersection(Range.closed(6, 8)) shouldBe Range.closed(6, 8)

    // equal
    range.intersection(range) shouldBe range

    // enclosing with same start
    range.intersection(Range.closed(4, 10)) shouldBe range

    // enclosing with same end
    range.intersection(Range.closed(2, 8)) shouldBe range

    // enclosing, exterior
    range.intersection(Range.closed(2, 10)) shouldBe range

    // overlap above
    range.intersection(Range.closed(6, 10)) shouldBe Range.closed(6, 8)

    // adjacent above
    range.intersection(Range.openClosed(8, 10)) shouldBe Range.openClosed(8, 8)

    // separate above
    an[IllegalArgumentException] shouldBe thrownBy {
      range.intersection(Range.closed(10, 12))
    }
  }
}
