package fr.cnrs.liris.testing

import org.scalatest.matchers.{BeMatcher, MatchResult}

trait CustomMatchers {

  class CloseToMatcher(expectedValue: Double, tolerance: Double) extends BeMatcher[Double] {

    override def apply(left: Double) = MatchResult(
      left >= expectedValue - tolerance && left <= expectedValue + tolerance,
      s"Value $left was not close to $expectedValue (+/- $tolerance).",
      s"Value $left was close to $expectedValue (+/- $tolerance)."
    )
  }

  def closeTo(expectedValue: Double, tolerance: Double) = new CloseToMatcher(expectedValue, tolerance)
}
