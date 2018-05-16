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

private[sparkle] class JoinDataFrame[T, U, V](
  first: DataFrame[(String, Iterable[T])],
  other: DataFrame[(String, Iterable[U])],
  fn: (String, Iterable[T], Iterable[U]) => Iterable[V],
  private[sparkle] val encoder: Encoder[V])
  extends DataFrame[V] {

  override def toString: String = MoreObjects.toStringHelper(this).addValue(first).addValue(other).toString

  override private[sparkle] def keys: Seq[String] = first.keys

  override private[sparkle] def env: SparkleEnv = first.env

  override private[sparkle] def load(key: String): Iterable[V] = {
    val firstPartition = first.load(key).toMap
    val otherPartition = other.load(key).toMap
    firstPartition
      .flatMap { case (k, v1) => fn(k, v1, otherPartition.getOrElse(k, Iterable.empty)) }
      .toSeq
  }
}