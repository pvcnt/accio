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

import com.google.common.base.MoreObjects
import fr.cnrs.liris.privamov.core.io.DataSource

import scala.reflect.ClassTag

/**
 * A dataframe loading its data on the fly using a data source.
 *
 * @param source Data source.
 * @tparam T Elements' type.
 */
private[sparkle] class SourceDataFrame[T: ClassTag](source: DataSource[T], env: SparkleEnv) extends DataFrame[T](env) {
  override lazy val keys = source.keys

  override def load(key: String): Iterator[T] = source.read(key).iterator

  override def toString: String =
    MoreObjects.toStringHelper(this)
      .addValue(elementClassTag)
      .add("source", source)
      .toString
}
