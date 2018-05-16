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

import fr.cnrs.liris.sparkle.format._
import org.apache.commons.lang3.reflect.ConstructorUtils

import scala.reflect.ClassTag

private[sparkle] class ProductEncoder[T](val structType: StructType, cls: Class[T]) extends Encoder[T] {
  private[this] lazy val constructor = {
    val paramTypes = structType.fields.map(_._2).map {
      case DataType.Int32 => classOf[Int]
      case DataType.Int64 => classOf[Long]
      case DataType.Float32 => classOf[Float]
      case DataType.Float64 => classOf[Double]
      case DataType.String => classOf[String]
      case DataType.Bool => classOf[Boolean]
      case DataType.Time => classOf[org.joda.time.Instant]
    }

    // Finds an accessible constructor with compatible parameters. This is a more flexible search
    // than the exact matching algorithm in `Class.getConstructor`. The first assignment-compatible
    // matching constructor is returned.
    Option(ConstructorUtils.getMatchingAccessibleConstructor(cls, paramTypes: _*)).getOrElse {
      throw new RuntimeException(s"Couldn't find a valid constructor on $cls")
    }
  }

  override def classTag: ClassTag[T] = ClassTag(cls)

  override def serialize(obj: T): Iterable[InternalRow] = {
    val product = obj.asInstanceOf[Product]
    Iterable(InternalRow(product.productIterator.toArray))
  }

  override def deserialize(row: InternalRow): T = {
    if (row.fields.length != structType.fields.size) {
      throw new RuntimeException(s"Wrong number of fields: ${row.fields.length} (expected ${structType.fields.size})")
    }
    val args = row.fields.map(_.asInstanceOf[AnyRef])
    constructor.newInstance(args: _*)
  }
}