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

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.Files

import fr.cnrs.liris.sparkle.format.{DataType, InternalRow, StructType}
import fr.cnrs.liris.testing.UnitSpec
import org.joda.time.Instant
import org.scalactic.Equality

import scala.collection.JavaConverters._

/**
 * Unit tests for [[CsvDataFormat]].
 */
class CsvDataFormatSpec extends UnitSpec {
  behavior of "CsvDataFormat"

  private implicit val rowEquality = new Equality[InternalRow] {
    override def areEqual(a: InternalRow, b: Any): Boolean =
      b match {
        case row: InternalRow => a.fields.deep == row.fields.deep
        case _ => false
      }
  }

  it should "read a CSV file" in {
    val is = new FileInputStream("accio/javatests/fr/cnrs/liris/sparkle/format/csv/foo.csv")
    val structType = StructType(Seq(
      "s" -> DataType.String,
      "b" -> DataType.Bool,
      "f" -> DataType.Float32,
      "d" -> DataType.Float64,
      "l" -> DataType.Int64,
      "i" -> DataType.Int32,
      "t" -> DataType.Time))
    val rows = try {
      CsvDataFormat.readerFor(structType).read(is).toList
    } finally {
      is.close()
    }
    rows should contain theSameElementsInOrderAs Seq(
      InternalRow(Array("foo", true, 3.15f, 3.14d, 2L, 30, new Instant(1234567890))),
      InternalRow(Array("bar", true, 14.3f, 3.15d, 4L, 50, new Instant(1234567891))),
      InternalRow(Array("foobar", false, 42.0f, 14.3d, 6L, 70, new Instant(1234567892))),
      InternalRow(Array("bar foo", false, 3.14f, 42d, 70L, 6, new Instant(1234567893))))
  }

  it should "write a CSV file" in {
    val tmpFile = Files.createTempFile(getClass.getSimpleName, ".csv")
    val os = new FileOutputStream(tmpFile.toFile)
    val structType = StructType(Seq(
      "s" -> DataType.String,
      "b" -> DataType.Bool,
      "f" -> DataType.Float32,
      "d" -> DataType.Float64,
      "l" -> DataType.Int64,
      "i" -> DataType.Int32,
      "t" -> DataType.Time))
    val rows = Seq(
      InternalRow(Array("foo", true, 3.15f, 3.14d, 2L, 30, new Instant(1234567890))),
      InternalRow(Array("bar", true, 14.3f, 3.15d, 4L, 50, new Instant(1234567891))),
      InternalRow(Array("foobar", false, 42.0f, 14.3d, 6L, 70, new Instant(1234567892))),
      InternalRow(Array("bar foo", false, 3.14f, 42d, 70L, 6, new Instant(1234567893))))
    try {
      CsvDataFormat.writerFor(structType).write(rows, os)
    } finally {
      os.close()
    }
    Files.readAllLines(tmpFile).asScala should contain theSameElementsInOrderAs Seq(
      "s,b,f,d,l,i,t",
      "foo,true,3.15,3.14,2,30,1234567890",
      "bar,true,14.3,3.15,4,50,1234567891",
      "foobar,false,42.0,14.3,6,70,1234567892",
      "bar foo,false,3.14,42.0,70,6,1234567893")
  }
}
