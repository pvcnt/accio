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

package fr.cnrs.liris.util.scrooge

private[scrooge] trait ParsingContext {
  /**
   * Called before we write an item
   */
  def write(): Unit = {}

  /**
   * Thrift maps are made up of name value pairs, are we parsing a
   * thrift map name (e.g. left hand side of a map entry) here?
   */
  def isMapKey: Boolean = false

  def indent: Int = 0
}

private[scrooge] object ParsingContext {

  final class Map extends ParsingContext {
    private[this] var lhs = false

    override def write(): Unit = lhs = !lhs

    override def isMapKey: Boolean = lhs

    override def indent: Int = 1
  }

  final class Struct extends ParsingContext {
    override def indent: Int = 1
  }

  final class Sequence extends ParsingContext {
    override def indent: Int = 1
  }

  object Null extends ParsingContext

}