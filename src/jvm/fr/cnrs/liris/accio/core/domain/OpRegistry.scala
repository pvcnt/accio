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

package fr.cnrs.liris.accio.core.domain

/**
 * Registry providing definitions of all operators known to Accio.
 */
trait OpRegistry {
  /**
   * Return all operator definitions.
   */
  def ops: Set[OpDef]

  /**
   * Check whether the registry contains an operator with given name.
   *
   * @param name Operator name.
   */
  def contains(name: String): Boolean

  /**
   * Return the definition of a given operator, if it exists.
   *
   * @param name Operator name.
   */
  def get(name: String): Option[OpDef]

  /**
   * Return the definition of a given operator.
   *
   * @param name Operator name.
   * @throws NoSuchElementException If there is no operator with the given name.
   */
  @throws[NoSuchElementException]
  def apply(name: String): OpDef
}