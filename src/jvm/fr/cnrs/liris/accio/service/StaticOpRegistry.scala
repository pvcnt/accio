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

package fr.cnrs.liris.accio.service

import fr.cnrs.liris.accio.api.thrift.OpDef

/**
 * Operator registry embedding static definitions (i.e., known when instantiating the object).
 */
class StaticOpRegistry(defns: Set[OpDef]) extends OpRegistry {
  private[this] val index = defns.map(opDef => opDef.name -> opDef).toMap

  override def ops: Set[OpDef] = index.values.toSet

  override def contains(name: String): Boolean = index.contains(name)

  override def get(name: String): Option[OpDef] = index.get(name)

  override def apply(name: String): OpDef = index(name)
}