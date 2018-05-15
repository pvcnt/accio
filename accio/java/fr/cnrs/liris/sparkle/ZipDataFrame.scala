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

private[sparkle] class ZipDataFrame[T, U, V](
  first: DataFrame[(String, Seq[T])],
  other: DataFrame[(String, Seq[U])],
  fn: (String, Seq[T], Seq[U]) => Seq[V],
  private[sparkle] val encoder: Encoder[V])
  extends DataFrame[V] {

  override def keys: Seq[String] = first.keys

  override def toString: String = MoreObjects.toStringHelper(this).addValue(first).addValue(other).toString

  override private[sparkle] def env: SparkleEnv = first.env

  override private[sparkle] def load(key: String): Seq[V] = {
    val firstPartition = first.load(key).toMap
    val otherPartition = other.load(key).toMap
    firstPartition
      .flatMap { case (k, v1) => fn(k, v1, otherPartition.getOrElse(k, Seq.empty)) }
      .toSeq
  }
}