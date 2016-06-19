package fr.cnrs.liris.accio.core.framework

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[Objective]].
 */
class ObjectiveSpec extends UnitSpec {
  "Objective" should "parse a minimization objective" in {
    Objective.parse("Minimize(foo)") shouldBe Objective.Minimize("foo", None)
  }

  it should "ignore spaces when parsing a minimization objective" in {
    Objective.parse(" Minimize(foo)") shouldBe Objective.Minimize("foo", None)
    Objective.parse("Minimize(foo) ") shouldBe Objective.Minimize("foo", None)
    Objective.parse("Minimize( foo)") shouldBe Objective.Minimize("foo", None)
    Objective.parse("Minimize(foo )") shouldBe Objective.Minimize("foo", None)
  }

  it should "parse a maximization objective" in {
    Objective.parse("Maximize(foo)") shouldBe Objective.Maximize("foo", None)
  }

  it should "ignore spaces when parsing a maximization objective" in {
    Objective.parse(" Maximize(foo)") shouldBe Objective.Maximize("foo", None)
    Objective.parse("Maximize(foo) ") shouldBe Objective.Maximize("foo", None)
    Objective.parse("Maximize( foo)") shouldBe Objective.Maximize("foo", None)
    Objective.parse("Maximize(foo )") shouldBe Objective.Maximize("foo", None)
  }

  it should "parse a comparison objective" in {
    val n = Math.random()
    Objective.parse(s"Compare(foo == $n)") shouldBe Objective.EqualTo("foo", n)
    Objective.parse(s"Compare(foo <= $n)") shouldBe Objective.LessThan("foo", n)
    Objective.parse(s"Compare(foo >= $n)") shouldBe Objective.MoreThan("foo", n)
  }

  it should "ignore spaces when parsing a comparison objective" in {
    val n = Math.random()
    Objective.parse(s" Compare(foo == $n)") shouldBe Objective.EqualTo("foo", n)
    Objective.parse(s"Compare(foo == $n) ") shouldBe Objective.EqualTo("foo", n)
    Objective.parse(s"Compare(foo==$n)") shouldBe Objective.EqualTo("foo", n)
    Objective.parse(s"Compare(foo ==$n)") shouldBe Objective.EqualTo("foo", n)
    Objective.parse(s"Compare(foo== $n)") shouldBe Objective.EqualTo("foo", n)
    Objective.parse(s"Compare( foo==$n)") shouldBe Objective.EqualTo("foo", n)
    Objective.parse(s"Compare(foo==$n )") shouldBe Objective.EqualTo("foo", n)
  }

  it should "reject invalid string representations" in {
    an[IllegalArgumentException] shouldBe thrownBy {
      Objective.parse("Invalid(bar)")
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      Objective.parse("Minimize(foo")
    }
    an[IllegalArgumentException] shouldBe thrownBy {
      Objective.parse("Compare(foo = 42)")
    }
  }

  "Objective.Minimize" should "generate a parsable string representation" in {
    val obj = Objective.Minimize("foo", None)
    Objective.parse(obj.toString) shouldBe obj
  }

  "Objective.Maximize" should "generate a parsable string representation" in {
    val obj = Objective.Maximize("foo", None)
    Objective.parse(obj.toString) shouldBe obj
  }

  "Objective.EqualTo" should "generate a parsable string representation" in {
    val obj = Objective.EqualTo("foo", Math.random())
    Objective.parse(obj.toString) shouldBe obj
  }

  "Objective.LessThan" should "generate a parsable string representation" in {
    val obj = Objective.LessThan("foo", Math.random())
    Objective.parse(obj.toString) shouldBe obj
  }

  "Objective.MoreThan" should "generate a parsable string representation" in {
    val obj = Objective.MoreThan("foo", Math.random())
    Objective.parse(obj.toString) shouldBe obj
  }
}
