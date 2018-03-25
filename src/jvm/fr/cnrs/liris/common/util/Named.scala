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

package fr.cnrs.liris.common.util

/**
 * Very simple interface to help dealing with named objects, whose name uniquely identifies them
 * among all instances of this class.
 */
trait Named {
  /**
   * Return this object's name, which should be unique among all instances.
   *
   * @return A name
   */
  def name: String

  override def hashCode: Int = name.hashCode

  override def equals(other: Any): Boolean = other match {
    case o: Named => o.name == name
    case _ => false
  }
}