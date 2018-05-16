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

package fr.cnrs.liris.sparkle.format

import java.io.{InputStream, OutputStream}

trait DataFormat {
  def extension: String

  def readerFor(structType: StructType, options: Map[String, String] = Map.empty): RowReader

  def writerFor(structType: StructType, options: Map[String, String] = Map.empty): RowWriter
}

trait RowReader {
  /**
   *
   * The reader is expected to close its resources (including the `is` input stream) once the end
   * of the stream is reached, or when an error occurred.
   *
   * @param is
   * @return
   */
  def read(is: InputStream): Iterable[InternalRow]
}

trait RowWriter {
  def write(rows: Iterable[InternalRow], os: OutputStream): Unit
}