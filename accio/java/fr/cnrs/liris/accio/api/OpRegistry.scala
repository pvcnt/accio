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

package fr.cnrs.liris.accio.api

import fr.cnrs.liris.accio.api.thrift.Operator

/**
 * Registry providing definitions of all operators known to Accio.
 */
final class OpRegistry(val ops: Set[Operator]) {
  private[this] val index = ops.map(opDef => opDef.name -> opDef).toMap

  def contains(name: String): Boolean = index.contains(name)

  def get(name: String): Option[Operator] = index.get(name)

  def apply(name: String): Operator = index(name)
}