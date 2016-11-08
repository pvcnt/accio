/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.common.util

import java.text.BreakIterator

object StringUtils {
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
