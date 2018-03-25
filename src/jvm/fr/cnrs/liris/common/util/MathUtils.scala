/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.common.util

/**
 * Utils dealing with numbers.
 */
object MathUtils {
  /**
   * Return a float number rounded at one decimal. Faster than the generic [[roundAt]] method.
   *
   * @param v Number to round.
   */
  def roundAt1(v: Double): Double = (v * 10).round.toDouble / 10

  /**
   * Return a float number rounded at two decimals. Faster than the generic [[roundAt]] method.
   *
   * @param v Number to round.
   */
  def roundAt2(v: Double): Double = (v * 100).round.toDouble / 100

  /**
   * Return a float number rounded at three decimals. Faster than the generic [[roundAt]] method.
   *
   * @param v Number to round.
   */
  def roundAt3(v: Double): Double = (v * 1000).round.toDouble / 1000

  /**
   * Return a float number rounded at four decimals. Faster than the generic [[roundAt]] method.
   *
   * @param v Number to round.
   */
  def roundAt4(v: Double): Double = (v * 10000).round.toDouble / 10000

  /**
   * Return a float number rounded at five decimals. Faster than the generic [[roundAt]] method.
   *
   * @param v Number to round.
   */
  def roundAt5(v: Double): Double = (v * 100000).round.toDouble / 100000

  /**
   * Return a float number rounded at six decimals. Faster than the generic [[roundAt]] method.
   *
   * @param v Number to round.
   */
  def roundAt6(v: Double): Double = (v * 1000000).round.toDouble / 1000000

  /**
   * Return a float number rounded at a given number of decimals. If you know at compile time the number of decimals
   * you need, use one of the [[roundAt1]] to [[roundAt6]] method will be faster (it avoids exponentiation).
   *
   * @param v      Number to round.
   * @param places Number of needed decimals.
   */
  def roundAt(v: Double, places: Int): Double = {
    val factor = math.pow(10, places)
    (v * factor).round.toDouble / factor
  }

  /**
   * Return the average value of a list of doubles.
   *
   * @param values Doubles to aggregate.
   */
  def mean(values: Iterable[Double]): Double = if (values.isEmpty) 0d else values.sum / values.size
}