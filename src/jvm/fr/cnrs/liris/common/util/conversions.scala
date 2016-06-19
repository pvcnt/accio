package fr.cnrs.liris.common.util

object conversions {

  class RichNumber(wrapped: Double) {
    def meter = meters

    def meters = Distance.meters(wrapped.abs)

    def kilometer = kilometers

    def kilometers = Distance.kilometers(wrapped.abs)

    def mile = miles

    def miles = Distance.miles(wrapped.abs)
  }

  implicit def intToRichNumber(i: Int): RichNumber = new RichNumber(i)

  implicit def longToRichNumber(l: Long): RichNumber = new RichNumber(l)

  implicit def doubleToRichNumber(d: Double): RichNumber = new RichNumber(d)
}
