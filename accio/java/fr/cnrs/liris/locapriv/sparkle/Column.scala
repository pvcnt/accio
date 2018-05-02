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

trait Column[T] {
  def name: String

  def dataType: DataType

  final def apply(idx: Int): T = values(idx)

  final def size: Int = values.length

  protected def values: Array[T]
}

final class Int32Column(val name: String, protected val values: Array[Int]) extends Column[Int] {
  override def dataType: DataType = DataType.Int32
}

final class Int64Column(val name: String, protected val values: Array[Long]) extends Column[Long] {
  override def dataType: DataType = DataType.Int64
}

final class Float32Column(val name: String, protected val values: Array[Float]) extends Column[Float] {
  override def dataType: DataType = DataType.Float32
}

final class Float64Column(val name: String, protected val values: Array[Double]) extends Column[Double] {
  override def dataType: DataType = DataType.Float64
}

final class StringColumn(val name: String, protected val values: Array[String]) extends Column[String] {
  override def dataType: DataType = DataType.String
}

final class BoolColumn(val name: String, protected val values: Array[Boolean]) extends Column[Boolean] {
  override def dataType: DataType = DataType.Bool
}

final class TimeColumn(val name: String, protected val values: Array[Timestamp]) extends Column[Timestamp] {
  override def dataType: DataType = DataType.Time
}