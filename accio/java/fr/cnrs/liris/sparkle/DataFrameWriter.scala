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
import fr.cnrs.liris.sparkle.format.csv.CsvDataFormat
import fr.cnrs.liris.sparkle.format.{DataFormat, RowWriter}

import scala.collection.mutable

final class DataFrameWriter[T](df: DataFrame[T]) {
  private[this] var _options = mutable.Map.empty[String, String]
  private[this] var _partitioner: Option[T => Any] = None

  def partitionBy(fn: T => Any): DataFrameWriter[T] = {
    _partitioner = Some(fn)
    this
  }

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
    if (df.keys.length == 1 && _partitioner.isEmpty) {
      write(uri, writer, df.load(df.keys.head))
    } else {
      df.foreachPartitionWithKey { case (key, elements) =>
        _partitioner match {
          case Some(partitioner) =>
            elements
              .groupBy(partitioner)
              .foreach { case (k, v) => write(s"$uri/$k#$key.csv", writer, v) }
          case None => write(s"$uri/$key.csv", writer, elements)
        }
      }
    }
  }

  def csv(uri: String): Unit = write(uri, CsvDataFormat)

  private def write(uri: String, writer: RowWriter, elements: Seq[T]): Unit = {
    val os = PosixFilesystem.createOutputStream(uri)
    try {
      writer.write(elements.map(df.encoder.serialize), os)
    } finally {
      os.close()
    }
  }
}
