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

import java.nio.charset.Charset

import com.google.common.base.Charsets
import fr.cnrs.liris.common.util.ByteUtils

import scala.reflect._

/**
 * A decoder that writes a file as a list of elements, each one being delimited by an end-of-line character ('\n').
 *
 * @param encoder Encoder to apply on each element.
 * @param charset Charset to use when writing a line.
 * @tparam T Type of elements being written.
 */
class TextLineEncoder[T: ClassTag](encoder: Encoder[T], charset: Charset = Charsets.UTF_8) extends Encoder[Seq[T]] {
  override def encode(obj: Seq[T]): Array[Byte] = {
    val lines = obj.map(encoder.encode)
    ByteUtils.foldLines(lines)
  }

  override def elementClassTag: ClassTag[Seq[T]] = classTag[Seq[T]]
}