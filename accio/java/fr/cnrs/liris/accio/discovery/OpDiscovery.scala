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

import com.twitter.util.Closable
import fr.cnrs.liris.accio.domain.Operator

/**
 * An operator discovery is in charge of finding available implementations of operators. Indeed,
 * as people writing workflows and people writing operators might be different, the workflows
 * writers should not have to care about specifying the definition of operators to be used and
 * their physical location. Instead, the Accio server uses an operator discovery implementation to
 * automatically find all available operators. This interface allows to use different strategies
 * towards that purpose.
 *
 * This is a low-level component that should never be directly accessed. Client code should instead
 * rely on an operator registry.
 */
trait OpDiscovery extends Closable {
  /**
   * Return all available operators. The order in which operators are returned depends on the
   * implementation and may not be deterministic.
   */
  def ops: Iterable[Operator]
}
