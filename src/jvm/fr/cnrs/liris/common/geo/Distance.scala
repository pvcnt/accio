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

/**
 * Value object representing a distance in various units. However, the internal representation
 * of a distance is done in meters.
 *
 * @param meters Distance value in meters.
 */
case class Distance(meters: Double) extends Ordered[Distance] {
  /**
   * Return the distance value in kilometers.
   */
  def kilometers: Double = meters / Distance.KmToMeters

  /**
   * Return the distance value in miles.
   */
  def miles: Double = meters / Distance.MileToMeters

  /**
   * Check if this distance corresponds to a finite one.
   *
   * @return True if the distance is finite, false otherwise.
   */
  def isFinite: Boolean = !meters.isInfinite

  /**
   * Check if this distance corresponds to an infinite one.
   *
   * @return True if the distance is infinite, false otherwise.
   */
  def isInfinite: Boolean = meters.isInfinite

  def /(factor: Double): Distance = new Distance(meters / factor)

  def /(that: Distance): Double = meters / that.meters

  def *(factor: Double): Distance = new Distance(meters * factor)

  def +(that: Distance): Distance = new Distance(meters + that.meters)

  def -(that: Distance): Distance = new Distance(meters - that.meters)

  def max(other: Distance): Distance = if (meters >= other.meters) this else other

  def min(other: Distance): Distance = if (meters <= other.meters) this else other

  def abs: Distance = new Distance(meters.abs)

  override def compare(that: Distance): Int = meters.compareTo(that.meters)

  override def toString: String = s"$meters.meters"
}

/**
 * Factory for [[Distance]].
 */
object Distance {
  val MileToMeters = 1609.344
  val KmToMeters = 1000d

  val Zero = new Distance(0d)
  val Infinity = new Distance(Double.PositiveInfinity)

  /**
   * Parse a string into a distance.
   *
   * @param str String to parse.
   * @throws IllegalArgumentException If the string is not formatted as a valid distance.
   */
  def parse(str: String): Distance = str.split("\\.") match {
    case Array(v, u) => new Distance(v.toDouble * factor(u))
    case Array(v1, v2, u) => new Distance(s"$v1.$v2".toDouble * factor(u))
    case _ => throw new NumberFormatException(s"Invalid distance string: $str")
  }

  /**
   * Create a distance from a value in meters.
   *
   * @param meters Distance value in meters.
   */
  def meters(meters: Double): Distance = new Distance(meters)

  /**
   * Create a distance from a value in kilometers.
   *
   * @param kilometers Distance value in kilometers.
   */
  def kilometers(kilometers: Double): Distance = new Distance(kilometers * KmToMeters)

  /**
   * Create a distance from a value in miles.
   *
   * @param miles Distance value in miles.
   */
  def miles(miles: Double): Distance = new Distance(miles * MileToMeters)

  private def factor(str: String): Double = {
    var lower = str.toLowerCase
    if (lower.endsWith("s")) {
      lower = lower.dropRight(1)
    }
    lower match {
      case "meter" => 1d
      case "kilometer" => KmToMeters
      case "mile" => MileToMeters
      case badUnit => throw new NumberFormatException(s"Unrecognized distance unit $badUnit")
    }
  }
}