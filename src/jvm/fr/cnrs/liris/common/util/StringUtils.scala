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

package fr.cnrs.liris.common.util

import java.text.BreakIterator

import com.google.common.escape.{CharEscaperBuilder, Escaper}

/**
 * Utils for dealing with strings and text.
 */
object StringUtils {
  val QuotesEscaper: Escaper = new CharEscaperBuilder().addEscape('"', "\\\"").addEscape('\\', "\\\\").toEscaper
  val DoubleQuoteEscaper: Escaper = new CharEscaperBuilder().addEscape('"', "\\\"").toEscaper
  val SingleQuoteEscaper: Escaper = new CharEscaperBuilder().addEscape('\\', "\\\\").toEscaper

  /**
   * Paragraph-fill the specified input text, indenting lines to 'indent' and
   * wrapping lines at 'width'.  Returns the formatted result.
   */
  def paragraphFill(in: String, width: Int, indent: Int = 0): String = {
    val indentString = " " * indent
    val out = new StringBuilder
    var sep = ""
    in.split("\n").foreach { paragraph =>
      val boundary = BreakIterator.getLineInstance // (factory)
      boundary.setText(paragraph)
      out.append(sep).append(indentString)
      var cursor = indent
      var start = boundary.first()
      var end = boundary.next()
      while (end != BreakIterator.DONE) {
        val word = paragraph.substring(start, end) // (may include trailing space)
        if (word.length() + cursor > width) {
          out.append('\n').append(indentString)
          cursor = indent
        }
        out.append(word)
        cursor += word.length()
        start = end
        end = boundary.next()
      }
      sep = "\n";
    }
    out.toString
  }

  def truncate(str: String, length: Int): String = {
    if (length < 0 || str.length <= length) str else str.take(length - 2) + ".."
  }

  def maybe(str: String): Option[String] = if (str.isEmpty) None else Some(str)

  def explode(str: String, delimiter: String): Set[String] = str.split(delimiter).map(_.trim).toSet

  def explode(str: String): Set[String] = explode(str, ",")

  def explode(str: Option[String], delimiter: String): Set[String] =
    str.map(explode(_, delimiter)).getOrElse(Set.empty[String])
}
