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

import fr.cnrs.liris.sparkle.filesystem.{Filesystem, DefaultFilesystem}
import fr.cnrs.liris.sparkle.format.csv.CsvDataFormat
import fr.cnrs.liris.sparkle.format.{DataFormat, RowWriter}

import scala.collection.mutable

final class DataFrameWriter[T](df: DataFrame[T]) {
  private[this] var _options = mutable.Map.empty[String, String]

  def option(key: String, value: String): DataFrameWriter[T] = {
    _options(key) = value
    this
  }

  def option(key: String, value: Char): DataFrameWriter[T] = {
    _options(key) = value.toString
    this
  }

  def option(key: String, value: Boolean): DataFrameWriter[T] = {
    _options(key) = value.toString
    this
  }

  def options(options: Map[String, String]): DataFrameWriter[T] = {
    _options ++= options
    this
  }

  def csv(uri: String): Unit = write(uri, CsvDataFormat, DefaultFilesystem)

  def write(uri: String, format: DataFormat, filesystem: Filesystem): Unit = {
    val writer = format.writerFor(df.encoder.structType, _options.toMap)
    val extension = if (format.extension.nonEmpty) '.' + format.extension else ""
    df.env.submit[T, Unit](df, df.keys) { (key, elements) =>
      val dest = if (key == ".") uri else s"$uri/$key$extension"
      write(dest, writer, filesystem, elements)
    }
  }

  private def write(uri: String, writer: RowWriter, filesystem: Filesystem, elements: Iterable[T]): Unit = {
    val os = filesystem.createOutputStream(uri)
    try {
      writer.write(elements.flatMap(df.encoder.serialize), os)
    } finally {
      os.close()
    }
  }
}
