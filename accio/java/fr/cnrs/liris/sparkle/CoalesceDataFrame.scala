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

private[sparkle] class CoalesceDataFrame[T](inner: DataFrame[T])
  extends DataFrame[T] {

  override private[sparkle] def keys: Seq[String] = Seq("0")

  override private[sparkle] def load(key: String): Iterable[T] = {
    Iterable.concat(inner.keys.map(inner.load): _*)
  }

  override private[sparkle] def env = inner.env

  override private[sparkle] def encoder = inner.encoder
}
