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

import java.nio.charset.Charset

import com.google.common.base.Charsets

import scala.reflect._

/**
 * A decoder handling files containing a list of elements, each one being delimited by an end-of-line character ('\n').
 *
 * @param headerLines Number of header lines to ignore.
 * @param charset     Charset to use when decoding a line.
 */
final class TextLineCodec(headerLines: Int = 0, charset: Charset = Charsets.UTF_8) extends Codec[String] {
  override def elementClassTag: ClassTag[String] = classTag[String]

  override def decode(key: String, bytes: Array[Byte]): Seq[String] = {
    new String(bytes, charset)
      .split("\n")
      .toSeq
      .drop(headerLines)
  }

  override def encode(key: String, elements: Seq[String]): Array[Byte] = {
    elements.mkString("\n").getBytes(charset)
  }
}