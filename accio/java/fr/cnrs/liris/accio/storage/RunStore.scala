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

package fr.cnrs.liris.accio.storage

import fr.cnrs.liris.accio.api.ResultList
import fr.cnrs.liris.accio.api.thrift._

/**
 * Repository giving access to runs.
 */
trait RunStore {
  /**
   * Search for runs matching a given query. Runs are returned ordered in inverse chronological
   * order, the most recent matching run being the first result. It does *not* include the result
   * of each node.
   *
   * @param query Query.
   */
  def list(query: RunQuery): ResultList[Run]

  /**
   * Retrieve a specific run, if it exists.
   *
   * @param id Run identifier.
   */
  def get(id: RunId): Option[Run]

  /**
   * Retrieve the cached result of an operator, if it exists. It is not mandatory for
   * implementations to handle this operator.
   *
   * @param cacheKey Cache key.
   */
  def get(cacheKey: CacheKey): Option[NodeStatus]
}

object RunStore {

  /**
   * Mutable run repository.
   *
   * Mutating methods are *not* required to be thread-safe in the sense they will be wrapped inside
   * transactions at the application-level. However, they should still take care not to leave data in
   * a corrupted state, which can be hard to recover from.
   */
  trait Mutable extends RunStore {
    /**
     * Save a run. It will either create a new run or replace an existing one with the same identifier.
     *
     * @param run Run to save.
     */
    def save(run: Run): Unit

    /**
     * Delete a run, if it exists. It does *not* remove child runs, it is up to client code to do this.
     *
     * @param id Run identifier.
     */
    def remove(id: RunId): Unit
  }

}