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

import fr.cnrs.liris.sparkle.filesystem.PosixFilesystem
import fr.cnrs.liris.sparkle.format.DataFormat
import fr.cnrs.liris.sparkle.format.csv.CsvDataFormat

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

  def write(uri: String, format: DataFormat): Unit = {
    val writer = format.writerFor(df.encoder.structType, _options.toMap)
    if (df.keys.length == 1) {
      val os = PosixFilesystem.createOutputStream(uri)
      try {
        writer.write(df.load(df.keys.head).map(df.encoder.serialize), os)
      } finally {
        os.close()
      }
    } else {
      df.foreachPartitionWithKey { case (key, elements) =>
        val os = PosixFilesystem.createOutputStream(s"$uri/$key.csv")
        val writer = format.writerFor(df.encoder.structType, _options.toMap)
        try {
          writer.write(elements.map(df.encoder.serialize), os)
        } finally {
          os.close()
        }
      }
    }
  }

  def csv(uri: String): Unit = write(uri, CsvDataFormat)
}
