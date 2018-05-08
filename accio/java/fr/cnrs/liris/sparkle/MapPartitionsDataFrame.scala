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

import scala.reflect.ClassTag

private[sparkle] class MapPartitionsDataFrame[T, U: ClassTag](
  inner: DataFrame[T],
  fn: (String, Seq[T]) => Seq[U])
  extends DataFrame[U] {

  override def keys: Seq[String] = inner.keys

  override def toString: String = MoreObjects.toStringHelper(this).addValue(inner).toString

  override private[sparkle] def load(key: String): Seq[U] = fn(key, inner.load(key))

  override private[sparkle] def env = inner.env
}
