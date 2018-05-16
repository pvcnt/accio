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

import scala.reflect.ClassTag

private[sparkle] class PrimitiveEncoder[T](val dataType: DataType, cls: Class[T])
  extends Encoder[T] {

  override lazy val structType: StructType = StructType(Seq("value" -> dataType))

  override def classTag: ClassTag[T] = ClassTag(cls)

  override def serialize(obj: T): Iterable[InternalRow] = {
    Iterable(InternalRow(Array(obj)))
  }

  override def deserialize(row: InternalRow): T = {
    row.fields.head.asInstanceOf[T]
  }
}