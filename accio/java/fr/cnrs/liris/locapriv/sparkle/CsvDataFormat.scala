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

package fr.cnrs.liris.locapriv.sparkle

import java.io.{InputStream, OutputStream}

import com.univocity.parsers.common.processor.ColumnProcessor
import com.univocity.parsers.csv.{CsvParser, CsvParserSettings}

import scala.collection.JavaConverters._
import scala.util.Try

object CsvDataFormat extends DataFormat {
  override def read(is: InputStream, schema: Option[Schema]): Frame = {
    val processor = new ColumnProcessor
    val settings = new CsvParserSettings
    settings.setProcessor(processor)
    settings.getFormat.setDelimiter(',')
    settings.getFormat.setLineSeparator("\n")
    settings.setHeaderExtractionEnabled(true)
    schema.foreach(s => settings.selectFields(s.fields.map(_._1): _*))

    val parser = new CsvParser(settings)
    parser.parse(is)

    if (processor.getHeaders.isEmpty) {
      Frame.empty
    } else {
      val columns = schema match {
        case Some(s) =>
          s.fields.map {
            case (name, DataType.Int32) =>
              val values = processor.getColumn(name).asScala.map(parseInt32).toArray
              new Int32Column(name, values)
            case (name, DataType.Int64) =>
              val values = processor.getColumn(name).asScala.map(parseInt64).toArray
              new Int64Column(name, values)
            case (name, DataType.Float32) =>
              val values = processor.getColumn(name).asScala.map(parseFloat32).toArray
              new Float32Column(name, values)
            case (name, DataType.Float64) =>
              val values = processor.getColumn(name).asScala.map(parseFloat64).toArray
              new Float64Column(name, values)
            case (name, DataType.String) =>
              val values = processor.getColumn(name).asScala.toArray
              new StringColumn(name, values)
            case (name, DataType.Bool) =>
              val values = processor.getColumn(name).asScala.map(parseBool).toArray
              new BoolColumn(name, values)
            case (name, DataType.Time) =>
              val values = processor.getColumn(name).asScala.map(parseTime).toArray
              new TimeColumn(name, values)
          }
        case None =>
          val names = processor.getHeaders
          names.toSeq.map(name => new StringColumn(name, processor.getColumn(name).asScala.toArray))
      }
      new Frame(columns)
    }
  }

  override def write(frame: Frame, os: OutputStream): Unit = ???

  private def parseInt32(str: String): Int = {
    if (str.isEmpty) 0 else Try(str.toInt).getOrElse(0)
  }

  private def parseInt64(str: String): Long = {
    if (str.isEmpty) 0L else Try(str.toLong).getOrElse(0L)
  }

  private def parseFloat32(str: String): Float = {
    if (str.isEmpty) 0f else Try(str.toFloat).getOrElse(0f)
  }

  private def parseFloat64(str: String): Double = {
    if (str.isEmpty) 0d else Try(str.toDouble).getOrElse(0d)
  }

  private def parseBool(str: String): Boolean =
    str match {
      case "true" | "t" | "yes" | "y" | "1" => true
      case "false" | "f" | "no" | "n" | "0" => false
      case _ => false
    }

  private def parseTime(str: String): Timestamp = Timestamp(parseInt64(str))
}