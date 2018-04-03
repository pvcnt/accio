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
import fr.cnrs.liris.accio.api.thrift.Workflow

/**
 * Repository giving access to workflows.
 */
trait WorkflowStore {
  /**
   * Search for workflows matching a given query. Runs are returned ordered in inverse
   * chronological order, the most recent matching workflow being the first result. It will only
   * consider the latest version of each workflow, and return the latest version of matching
   * workflow in results.
   *
   * @param query Query.
   */
  def list(query: WorkflowQuery): ResultList[Workflow]

  /**
   * Retrieve a specific workflow at its latest version, if it exists.
   *
   * @param id Workflow identifier.
   */
  def get(id: String, version: Option[String] = None): Option[Workflow]
}

object WorkflowStore {

  /**
   * Store providing read and write access to workflows.
   *
   * Mutating methods are *not* required to be thread-safe in the sense they will be wrapped inside
   * transactions at the storage-level. However, they should still take care not to leave data in
   * a corrupted state, which can be hard to recover from.
   *
   * Note: For now, there is intentionally no method to remove a workflow, because it is not
   * desirable to delete a workflow with runs referencing it. This might evolve in the future if
   * we find an elegant solution to handle this.
   */
  trait Mutable extends WorkflowStore {
    /**
     * Save a workflow. It will either create a new workflow or a new version if there is already
     * one with the same identifier. Workflows are never updated.
     *
     * @param workflow Workflow to save.
     */
    def save(workflow: Workflow): Unit
  }

}