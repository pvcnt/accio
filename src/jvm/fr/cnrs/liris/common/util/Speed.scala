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

import com.github.nscala_time.time.Imports._

/**
 * Value object representing a speed in various units. However, the internal representation
 * of a distance is done in meters per second.
 *
 * @param metersPerSec A speed value in meters per second
 */
case class Speed(metersPerSec: Double) extends Ordered[Speed] {
  /**
   * Return the speed value in kilometers per hour.
   *
   * @return A speed in kilometers per hour
   */
  def kilometersPerHour: Double = metersPerSec * Speed.HourToSecs / Distance.KmToMeters

  /**
   * Return the speed value in miles per hour.
   *
   * @return A speed in miles per hour
   */
  def milesPerHour: Double = metersPerSec * Speed.HourToSecs / Distance.MileToMeters
  
  def +(speed: Speed): Speed = new Speed(metersPerSec + speed.metersPerSec)

  def /(factor: Double): Speed = new Speed(metersPerSec / factor)

  def *(factor: Double): Speed = new Speed(metersPerSec * factor)

  override def compare(that: Speed): Int = metersPerSec.compareTo(that.metersPerSec)

  override def toString: String = s"$metersPerSec.m.s"
}

/**
 * Factory for [[Speed]].
 */
object Speed {
  val Zero = metersPerSec(0)
  val HourToSecs = 3600

  /**
   * Parse a string into a speed.
   *
   * @param string A string to parse
   * @return A new speed
   */
  def parse(string: String): Speed = {
    val trimmed = string.trim
    try {
      metersPerSec(trimmed.toDouble)
    } catch {
      case e: NumberFormatException =>
        if (trimmed.endsWith(".mph")) {
          milesPerHour(trimmed.substring(0, trimmed.length - 4).toDouble)
        } else if (trimmed.endsWith(".km.h")) {
          kilometersPerHour(trimmed.substring(0, trimmed.length - 5).toDouble)
        } else if (trimmed.endsWith(".m.s")) {
          metersPerSec(trimmed.substring(0, trimmed.length - 4).toDouble)
        } else {
          throw new IllegalArgumentException(s"Invalid speed: $string")
        }
    }
  }

  def apply(distance: Distance, duration: Duration): Speed =
    new Speed(distance.meters / (duration.millis / 1000))

  /**
   * Create a speed from a value in meters per second.
   *
   * @param metersPerSec A speed in meters per second
   * @return A new speed
   */
  def metersPerSec(metersPerSec: Double): Speed = new Speed(metersPerSec)

  /**
   * Create a speed from a value in kilometers per hour.
   *
   * @param kilometersPerHour A speed in kilometers per hour
   * @return A new speed
   */
  def kilometersPerHour(kilometersPerHour: Double): Speed =
    new Speed(kilometersPerHour * Distance.KmToMeters / HourToSecs)

  /**
   * Create a speed from a value in miles per hour.
   *
   * @param milesPerHour A speed in miles per hour
   * @return A new speed
   */
  def milesPerHour(milesPerHour: Double): Speed = new Speed(milesPerHour * Distance.MileToMeters / HourToSecs)
}
