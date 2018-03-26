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

package fr.cnrs.liris.accio.tools.cli.event

import scala.util.matching.Regex

/**
 * An output filter for warnings.
 */
trait OutputFilter {
  /**
   * Returns true iff the given tag matches the output filter.
   */
  def showOutput(tag: String): Boolean
}

object OutputFilter {
  /**
   * An output filter that matches everything.
   */
  object Everything extends OutputFilter {
    override def showOutput(tag: String): Boolean = true
  }

  /**
   * An output filter that matches nothing.
   */
  object Nothing extends OutputFilter {
    override def showOutput(tag: String): Boolean = false
  }

}

/**
 * An output filter using regular expression matching.
 */
final class RegexOutputFilter(regex: Regex) extends OutputFilter {
  override def showOutput(tag: String): Boolean = regex.findFirstIn(tag).isDefined
}