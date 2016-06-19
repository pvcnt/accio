/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.common.util

/**
 * Value object representing a distance in various units. However, the internal representation
 * of a distance is done in meters.
 *
 * @param meters A distance in meters
 */
case class Distance(meters: Double) extends Ordered[Distance] {
  /**
   * Return the distance in kilometers.
   *
   * @return A distance in kilometers
   */
  def kilometers: Double = meters / Distance.KmToMeters

  /**
   * Return the distance in miles.
   *
   * @return A distance in miles
   */
  def miles: Double = meters / Distance.MileToMeters

  /**
   * Check if this distance corresponds to a finite one.
   *
   * @return True if the distance is finite, false otherwise
   */
  def isFinite: Boolean = !meters.isInfinite

  /**
   * Check if this distance corresponds to an infinite one.
   *
   * @return True if the distance is infinite, false otherwise
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
   * @param str A string to parse
   * @return A new distance
   */
  def parse(str: String): Distance = str.split("\\.") match {
    case Array(v, u) => new Distance(v.toDouble * factor(u))
    case Array(v1, v2, u) => new Distance(s"$v1.$v2".toDouble * factor(u))
    case _ => throw new NumberFormatException(s"Invalid distance string: $str")
  }

  /**
   * Create a distance from a value in meters.
   *
   * @param meters A distance in meters
   * @return A new distance
   */
  def meters(meters: Double): Distance = new Distance(meters)

  /**
   * Create a distance from a value in kilometers.
   *
   * @param kilometers A distance in kilometers
   * @return A new distance
   */
  def kilometers(kilometers: Double): Distance = new Distance(kilometers * KmToMeters)

  /**
   * Create a distance from a value in miles.
   *
   * @param miles A distance in miles
   * @return A new distance
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