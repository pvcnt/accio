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
import fr.cnrs.liris.sparkle.filesystem.{Filesystem, PosixFilesystem}
import fr.cnrs.liris.sparkle.format.DataFormat

/**
 * A dataframe loading its data on the fly using a data source.
 *
 * @tparam T Elements' type.
 */
private[sparkle] class SourceDataFrame[T](
  uri: String,
  filesystem: Filesystem,
  format: DataFormat,
  options: Map[String, String],
  private[sparkle] val env: SparkleEnv,
  private[sparkle] val encoder: Encoder[T])
  extends DataFrame[T] {

  override val keys: Seq[String] = {
    val paths = filesystem.list(uri).toSeq
    if (paths.size == 1 && paths.head == uri) {
      Seq(".")
    } else {
      paths.map(_.drop(uri.length).stripPrefix("/"))
    }
  }

  override private[sparkle] def load(key: String) = {
    val path = if (key == ".") uri else s"$uri/$key"
    val is = PosixFilesystem.createInputStream(path)
    try {
      val reader = format.readerFor(encoder.structType, options)
      reader.read(is).map(encoder.deserialize).toSeq
    } finally {
      is.close()
    }
  }

  override def toString: String = MoreObjects.toStringHelper(this).add("format", format).toString
}
