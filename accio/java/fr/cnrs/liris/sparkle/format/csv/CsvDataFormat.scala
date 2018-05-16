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

package fr.cnrs.liris.sparkle.format.csv

import fr.cnrs.liris.sparkle.format.{DataFormat, RowReader, RowWriter, StructType}

object CsvDataFormat extends DataFormat {
  override def extension = "csv"

  override def readerFor(structType: StructType, options: Map[String, String]): RowReader = {
    new CsvRowReader(structType, CsvOptions.extract(options))
  }

  override def writerFor(structType: StructType, options: Map[String, String]): RowWriter = {
    new CsvRowWriter(structType, CsvOptions.extract(options))
  }
}
