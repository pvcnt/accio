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

import java.io.OutputStream

import com.univocity.parsers.csv.CsvWriter
import fr.cnrs.liris.sparkle.format.{DataType, InternalRow, RowWriter, StructType}
import org.joda.time.Instant

private[csv] class CsvRowWriter(structType: StructType, options: CsvOptions) extends RowWriter {
  private[this] type ValueConverter = Any => String
  private[this] val converters = {
    structType.fields.map { case (_, dataType) => createConverter(dataType) }.toArray
  }

  override def write(rows: Iterable[InternalRow], os: OutputStream): Unit = {
    val settings = options.asWriterSettings
    val writer = new CsvWriter(os, settings)
    if (options.header) {
      writer.writeHeaders(structType.fields.map(_._1): _*)
    }

    rows.foreach { row =>
      val values = row.fields.zipWithIndex.map { case (value, idx) => converters(idx).apply(value) }
      writer.writeRow(values)
    }
    writer.flush()
  }

  private def createConverter(dataType: DataType): ValueConverter = {
    dataType match {
      case DataType.Time => (value: Any) =>
        value.asInstanceOf[Instant].getMillis.toString
      case DataType.Float32 => (value: Any) =>
        value.asInstanceOf[Float] match {
          case Float.NaN => options.nanValue
          case Float.NegativeInfinity => options.negativeInf
          case Float.PositiveInfinity => options.positiveInf
          case f => f.toString
        }
      case DataType.Float64 => (value: Any) =>
        value.asInstanceOf[Double] match {
          case Double.NaN => options.nanValue
          case Double.NegativeInfinity => options.negativeInf
          case Double.PositiveInfinity => options.positiveInf
          case d => d.toString
        }
      case _ => (value: Any) => value.toString
    }
  }
}
