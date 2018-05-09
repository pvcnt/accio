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
import fr.cnrs.liris.sparkle.format.{DataFormat, Encoder}

import scala.collection.mutable

final class DataFrameReader[T](env: SparkleEnv, encoder: Encoder[T]) {
  self =>

  private[this] var _options = mutable.Map.empty[String, String]

  def option(key: String, value: String): DataFrameReader[T] = {
    _options(key) = value
    this
  }

  def option(key: String, value: Char): DataFrameReader[T] = {
    _options(key) = value.toString
    this
  }

  def option(key: String, value: Boolean): DataFrameReader[T] = {
    _options(key) = value.toString
    this
  }

  def options(options: Map[String, String]): DataFrameReader[T] = {
    _options ++= options
    this
  }

  def read(uri: String, format: DataFormat): DataFrame[T] = {
    val uris = PosixFilesystem.list(uri).toSeq
    new DataFrame[T] {
      override def keys: Seq[String] = uris

      override private[sparkle] def load(key: String) = {
        val is = PosixFilesystem.createInputStream(key)
        try {
          val reader = format.readerFor(encoder.structType, _options.toMap)
          reader.read(is).map(encoder.deserialize).toSeq
        } finally {
          is.close()
        }
      }

      override private[sparkle] def env = self.env

      override private[sparkle] def encoder = self.encoder
    }
  }

  def csv(uri: String): DataFrame[T] = read(uri, CsvDataFormat)
}
