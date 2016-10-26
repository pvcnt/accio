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

import java.nio.charset.Charset

import com.google.common.base.Charsets

/**
 * A decoder that reads a file as a list of elements, each one being delimited by an end-of-line character ('\n').
 * It therefore assumes textual content.
 *
 * @param decoder     Decoder to apply on each line.
 * @param headerLines Number of header lines to ignore.
 * @param charset     Charset to use when decoding a line.
 * @tparam T Elements' type.
 */
class TextLineDecoder[T](decoder: Decoder[T], headerLines: Int = 0, charset: Charset = Charsets.UTF_8) extends Decoder[Seq[T]] {
  override def decode(key: String, bytes: Array[Byte]): Option[Seq[T]] = {
    val lines = new String(bytes, charset).split("\n").drop(headerLines)
    val elements = lines.flatMap(line => decoder.decode(key, line.getBytes(charset)))
    if (elements.nonEmpty) Some(elements) else None
  }
}