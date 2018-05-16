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

/**
 * A data frame containing no elements.
 *
 * @param env Sparkle environment.
 * @tparam T Elements' type.
 */
private[sparkle] class EmptyDataFrame[T](
  private[sparkle] val env: SparkleEnv,
  private[sparkle] val encoder: Encoder[T])
  extends DataFrame[T] {

  override def toString: String = MoreObjects.toStringHelper(this).toString

  override private[sparkle] lazy val keys: Seq[String] = Seq.empty

  override private[sparkle] def load(key: String): Iterable[T] = Iterable.empty
}