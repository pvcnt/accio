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

/**
 * Parser for [[ExperimentDef]]s.
 */
trait ExperimentParser {
  /**
   * Parse a file into an experiment definition.
   *
   * @param uri URI to an experiment definition.
   */
  def parse(uri: String): ExperimentDef

  /**
   * Check whether a given URI could be read as an experiment definition.
   *
   * @param uri URI to (possibly) an experiment definition.
   * @return True if it could be read as an experiment definition, false otherwise
   */
  def canRead(uri: String): Boolean
}
