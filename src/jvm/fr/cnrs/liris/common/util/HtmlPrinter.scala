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

import java.io.PrintStream

/**
 * Utility function for writing HTML data to a [[PrintStream]].
 */
class HtmlPrinter(out: PrintStream) {
  /**
   * Print an open tag with attributes and possibly content and increase indentation level.
   *
   * All array elements are taken in pairs for attributes and their values. If odd, the last
   * element is taken as the content of the element. It is printed directly after the opening tag.
   *
   * @param tagName                  Tag name
   * @param attributesAndContent must have the form: attr1, value1, attr2, value2, ..., content
   */
  def tag(tagName: String, attributesAndContent: Any*)(fn: => Unit): Unit = {
    out.print(s"<$tagName")
    for (index <- 0 until (attributesAndContent.length - 1) by 2) {
      out.print(s""" ${attributesAndContent(index)}="${attributesAndContent(index + 1)}"""")
    }
    out.print(">")
    if (attributesAndContent.length % 2 == 1) {
      out.print(attributesAndContent.last)
    }
    fn
    out.print(s"</$tagName>")
  }

  /**
   * Print a single element with attributes and possibly content.
   */
  def element(tagName: String, attributesAndContent: Any*): Unit = {
    tag(tagName, attributesAndContent: _*)({})
  }
}

object HtmlPrinter {
  implicit def toHtmlPrinter(out: PrintStream): HtmlPrinter = new HtmlPrinter(out)
}