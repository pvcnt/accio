/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.privamov.core.io

import scala.reflect.ClassTag

/**
 * A sink is responsible for persisting elements. If they need to be read back later, you need to implement a
 * matching [[DataSource]].
 *
 * @tparam T Type of elements being written.
 */
trait DataSink[T] {
  /**
   * Persist some elements associated with the same key.
   *
   * @param key Key elements are associated to.
   * @param elements Elements to write.
   */
  def write(key: String, elements: TraversableOnce[T]): Unit
}

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