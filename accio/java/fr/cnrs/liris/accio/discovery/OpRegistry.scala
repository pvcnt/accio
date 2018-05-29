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

package fr.cnrs.liris.accio.discovery

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.domain.Operator

/**
 * The operator registry is a simple facade giving access to the discovered operators. Client
 * code should never access directly the underlying operator discovery implementation, but always
 * rely on this operator registry.
 *
 * @param discovery Operator discovery.
 */
@Singleton
final class OpRegistry @Inject()(discovery: OpDiscovery) {
  /**
   * Return all available operators. The order in which operators are returned depends on the
   * * implementation and may not be deterministic.
   */
  def ops: Iterable[Operator] = discovery.ops

  /**
   * Return an operator by its name, if it exists.
   *
   * @param name An operator name.
   */
  def get(name: String): Option[Operator] = ops.find(_.name == name)

  /**
   * Return an operator by its name.
   *
   * @param name An operator name.
   * @throws NoSuchElementException If no operator with this name exists.
   */
  def apply(name: String): Operator = get(name).get
}