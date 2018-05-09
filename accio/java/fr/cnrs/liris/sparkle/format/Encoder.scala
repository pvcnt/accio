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

import org.joda.time.Instant

import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

trait Encoder[T] {
  def structType: StructType

  def classTag: ClassTag[T]

  def serialize(obj: T): InternalRow

  def deserialize(row: InternalRow): T
}

object Encoder {
  implicit val Int32Encoder: Encoder[Int] = RowEncoder[Int]
  implicit val Int64Encoder: Encoder[Long] = RowEncoder[Long]
  implicit val Float32Encoder: Encoder[Float] = RowEncoder[Float]
  implicit val Float64Encoder: Encoder[Double] = RowEncoder[Double]
  implicit val StringEncoder: Encoder[String] = RowEncoder[String]
  implicit val BoolEncoder: Encoder[Boolean] = RowEncoder[Boolean]
  implicit val TimeEncoder: Encoder[Instant] = RowEncoder[Instant]

  implicit def structEncoder[T <: Product : TypeTag]: Encoder[T] = RowEncoder[T]
}