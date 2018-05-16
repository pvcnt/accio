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

import java.io.InputStream

import com.univocity.parsers.common.processor.RowListProcessor
import com.univocity.parsers.csv.CsvParser
import fr.cnrs.liris.sparkle.format._
import org.joda.time.Instant

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

private[csv] class CsvRowReader(structType: StructType, options: CsvOptions) extends RowReader {
  private[this] type ValueConverter = String => Any
  private[this] val converters = {
    structType.fields.map { case (_, dataType) => createConverter(dataType) }.toArray
  }

  override def read(is: InputStream): Iterable[InternalRow] = {
    val processor = new RowListProcessor
    val settings = options.asParserSettings
    settings.setProcessor(processor)
    settings.selectFields(structType.fields.map(_._1): _*)
    val parser = new CsvParser(settings)
    parser.parse(is)

    if (processor.getHeaders == null || processor.getHeaders.isEmpty) {
      Iterable.empty
    } else {
      /*val missing = structType.fields.map(_._1).filterNot(processor.getHeaders.contains)
      if (missing.nonEmpty) {
        throw new IllegalArgumentException(s"Missing columns: ${missing.mkString(", ")}")
      }
      val columns = structType.fields.map { case (name, _) => processor.getHeaders.indexOf(name) }.toArray*/
      processor.getRows.asScala.filter(_.nonEmpty).zipWithIndex.map { case (tokens, lineNo) =>
        val fields = structType.fields.indices.map { idx =>
          val token = tokens(idx)
          try {
            converters(idx).apply(token)
          } catch {
            case NonFatal(e) => throw new BadRecordException(token, Some(s"line $lineNo"), e)
          }
        }.toArray
        InternalRow(fields)
      }
    }
  }

  private def createConverter(dataType: DataType): ValueConverter = {
    dataType match {
      case DataType.Int32 => (str: String) =>
        str match {
          case null => 0
          case _ => str.toInt
        }
      case DataType.Int64 => (str: String) =>
        str match {
          case null => 0L
          case _ => str.toLong
        }
      case DataType.Float32 => (str: String) =>
        str match {
          case null => 0f
          case options.nanValue => Float.NaN
          case options.negativeInf => Float.NegativeInfinity
          case options.positiveInf => Float.PositiveInfinity
          case _ => str.toFloat
        }
      case DataType.Float64 => (str: String) =>
        str match {
          case null => 0d
          case options.nanValue => Double.NaN
          case options.negativeInf => Double.NegativeInfinity
          case options.positiveInf => Double.PositiveInfinity
          case _ => str.toDouble
        }
      case DataType.String => identity
      case DataType.Bool => (str: String) =>
        str match {
          case "true" | "t" | "yes" | "y" | "1" => true
          case "false" | "f" | "no" | "n" | "0" => false
          case _ => throw new IllegalArgumentException(s"Not a boolean: $str")
        }
      case DataType.Time => (str: String) => new Instant(str.toLong)
    }
  }
}