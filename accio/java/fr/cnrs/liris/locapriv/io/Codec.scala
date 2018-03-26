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

package fr.cnrs.liris.locapriv.io

import fr.cnrs.liris.common.util.ByteUtils

import scala.reflect.ClassTag

/**
 * A codec is the association of an encoder and a decoder of the same type.
 *
 * @tparam T Type of elements being read and written.
 */
trait Codec[T] extends Encoder[T] with Decoder[T]

/**
 * An encoder converts a plain object into bytes.
 *
 * @tparam T Type of elements being written.
 */
trait Encoder[T] {
  /**
   * Return the class tag of the element being written.
   */
  def elementClassTag: ClassTag[T]

  /**
   * Encode an object into a sequence of bytes.
   *
   * @param key      Key associated with the file being written.
   * @param elements Elements to encode.
   */
  def encode(key: String, elements: Seq[T]): Array[Byte]
}

/**
 * A decoder converts a binary (record) into a plain object.
 *
 * @tparam T Type of elements being read.
 */
trait Decoder[T] {
  /**
   * Return the class tag of the element being read.
   */
  def elementClassTag: ClassTag[T]

  /**
   * Decode a binary record into one or several objects.
   *
   * @param key   Key associated with the file being read.
   * @param bytes Binary content.
   */
  def decode(key: String, bytes: Array[Byte]): Seq[T]
}

import scala.reflect.{ClassTag, classTag}

/**
 * Codec doing nothing (reading and returning bytes).
 */
object IdentityCodec extends Codec[Array[Byte]] {
  override def elementClassTag: ClassTag[Array[Byte]] = classTag[Array[Byte]]

  override def encode(key: String, elements: Seq[Array[Byte]]): Array[Byte] = ByteUtils.foldLines(elements)

  override def decode(key: String, bytes: Array[Byte]): Seq[Array[Byte]] = Seq(bytes)
}