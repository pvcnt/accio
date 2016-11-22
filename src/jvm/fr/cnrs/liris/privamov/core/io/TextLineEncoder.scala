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

import scala.reflect._

/**
 * A decoder that writes a file as a list of elements, each one being delimited by an end-of-line character ('\n').
 *
 * @param encoder Encoder to apply on each element.
 * @param charset Charset to use when writing a line.
 * @tparam T Type of elements being written.
 */
class TextLineEncoder[T: ClassTag](encoder: Encoder[T], charset: Charset = Charsets.UTF_8) extends Encoder[Seq[T]] {
  private[this] val NL = "\n".getBytes(Charsets.UTF_8)

  override def encode(obj: Seq[T]): Array[Byte] = {
    val lines = obj.map(encoder.encode)
    foldLines(lines)
  }

  override def elementClassTag: ClassTag[Seq[T]] = classTag[Seq[T]]

  private def foldLines(lines: Seq[Array[Byte]]): Array[Byte] = {
    if (lines.isEmpty) {
      // No line, return directly an empty array.
      Array.empty[Byte]
    } else if (lines.size == 1) {
      // A single line, skip costly processing and return it directly.
      lines.head
    } else {
      // A simpler way to do this is: lines.foldLeft(Array.empty[Byte])(_ ++ NL ++ NL).
      // However, I got terribly bad performances doing this; it seems concatenating thousands of arrays is not
      // recommended, so I ended up doing it by end, allocating and filling a single byte array.
      val bytes = Array.ofDim[Byte](lines.map(_.length).sum + NL.length * (lines.size - 1))
      var offset = 0
      lines.zipWithIndex.foreach { case (line, idx) =>
        System.arraycopy(line, 0, bytes, offset, line.length)
        offset += line.length
        if (idx < lines.size - 1) {
          // It is not the last line, we add a line break.
          System.arraycopy(NL, 0, bytes, offset, NL.length)
          offset += NL.length
        }
      }
      bytes
    }
  }
}