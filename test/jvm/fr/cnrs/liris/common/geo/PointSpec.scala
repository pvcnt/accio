package fr.cnrs.liris.common.geo

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[Point]].
 */
class PointSpec extends UnitSpec {
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

    point2 = Point(4.14, 42)
    point1.distance(point2).meters shouldBe closeTo(1, 1e-13)

    point2 = Point(4.14, 50)
    point1.distance(point2).meters shouldBe closeTo(8.062257758, 1e-6)
  }

  it should "be convertable to coordinates" in {
    Point(3.14, 42).toSeq shouldBe Seq(3.14, 42)
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