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
 * A data frame reading its data from the memory.
 *
 * @param elements      Elements.
 * @param partitionSize Number of elements per partition.
 * @param env           Sparkle environment.
 * @tparam T Elements' type.
 */
private[sparkle] class MemoryDataFrame[T](
  elements: Seq[T],
  partitionSize: Int,
  private[sparkle] val env: SparkleEnv,
  private[sparkle] val encoder: Encoder[T])
  extends DataFrame[T] {

  require(partitionSize > 0, "Partition size cannot be null")

  override lazy val keys: Seq[String] = {
    Seq.tabulate(math.floor(elements.size.toDouble / partitionSize).toInt)(_.toString)
  }

  override def toString: String = MoreObjects.toStringHelper(this).toString

  override private[sparkle] def load(key: String): Iterable[T] = {
    val start = key.toInt * partitionSize
    elements.slice(start, math.min(start + partitionSize, elements.length))
  }
}