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

package fr.cnrs.liris.sparkle

import com.google.common.base.MoreObjects
import fr.cnrs.liris.util.random.XORShiftRandom

import scala.util.Random

private[sparkle] class SampleDataFrame[T](inner: DataFrame[T], fraction: Double, seed: Long)
  extends DataFrame[T] {

  require(fraction >= 0.0, s"Negative fraction value: $fraction")
  private[this] val seeds = {
    val random = new Random(seed)
    inner.keys.map(key => key -> random.nextLong).toMap
  }

  override def toString: String = MoreObjects.toStringHelper(this).addValue(inner).toString

  override private[sparkle] def keys: Seq[String] = inner.keys

  override private[sparkle] def load(key: String): Iterable[T] = {
    val elements = inner.load(key)
    if (fraction <= 0.0) {
      Iterable.empty
    } else if (fraction >= 1.0) {
      elements
    } else {
      val rng = new XORShiftRandom(seeds(key))
      elements.filter(_ => rng.nextDouble() <= fraction)
    }
  }

  override private[sparkle] def env = inner.env

  override private[sparkle] def encoder = inner.encoder
}