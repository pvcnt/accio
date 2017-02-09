/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.dal.core.io

import scala.reflect.ClassTag

trait Codec[T] extends Encoder[T] with Decoder[T]

/**
 * An encoder converts a plain object into bytes.
 *
 * @tparam T Type of elements being written.
 */
trait Encoder[T] {
  /**
   * Encode an object into a sequence of bytes.
   *
   * @param obj Plain object.
   * @return Binary content.
   */
  def encode(obj: T): Array[Byte]

  /**
   * Return the class tag of the element being written.
   */
  def elementClassTag: ClassTag[T]
}

/**
 * A decoder converts a binary (record) into a plain object.
 *
 * @tparam T Type of elements being read.
 */
trait Decoder[T] {
  /**
   * Decodes a binary record into an object.
   *
   * @param key   Key associated with the file containing this record
   * @param bytes Binary content
   * @return Plain object
   */
  def decode(key: String, bytes: Array[Byte]): Option[T]

  /**
   * Return the class tag of the element being read.
   */
  def elementClassTag: ClassTag[T]
}