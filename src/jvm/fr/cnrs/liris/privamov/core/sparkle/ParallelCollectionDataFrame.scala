/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.privamov.core.sparkle

import scala.reflect.ClassTag

/**
 * A data frame reading its data from the memory.
 *
 * @param data Data, indexed by key.
 * @param env  Sparkle environment.
 * @tparam T Elements' type.
 */
private[sparkle] class ParallelCollectionDataFrame[T: ClassTag](data: Map[String, Seq[T]], env: SparkleEnv) extends DataFrame[T](env) {
  override def keys: Seq[String] = data.keySet.toSeq

  override def load(key: String): Iterator[T] = if (data.contains(key)) data(key).iterator else Iterator.empty
}