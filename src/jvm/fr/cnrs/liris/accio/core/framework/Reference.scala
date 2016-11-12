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

package fr.cnrs.liris.accio.core.framework

import com.fasterxml.jackson.annotation.JsonValue

/**
 * A reference to a port of a node. It can be either an input or an output port, depending on the context where
 * this reference is specified.
 *
 * @param node Node name.
 * @param port Port name.
 */
case class Reference(node: String, port: String) {
  @JsonValue
  override def toString: String = s"$node${Reference.Delimiter}$port"
}

/**
 * Utils and factory for [[Reference]].
 */
object Reference {
  /**
   * Delimiter used to separate node and port in a dependency specification.
   */
  val Delimiter = "/"

  /**
   * Parse a string into a dependency.
   *
   * @param str String to parse.
   * @throws IllegalArgumentException If the string is not formatted as a valid reference.
   */
  @throws[IllegalArgumentException]
  def parse(str: String): Reference = str.split(Delimiter) match {
    case Array(node, port) =>
      require(node.nonEmpty, s"Invalid reference, empty node name: $str")
      require(port.nonEmpty, s"Invalid reference, empty port name: $str")
      Reference(node, port)
    case _ => throw new IllegalArgumentException(s"Invalid reference: $str")
  }
}
