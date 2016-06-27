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

package fr.cnrs.liris.accio.cli

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