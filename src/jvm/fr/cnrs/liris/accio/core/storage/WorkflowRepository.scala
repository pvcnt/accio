/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.storage

import fr.cnrs.liris.accio.core.domain.{Workflow, WorkflowId}

/**
 * Repository persisting workflows.
 */
trait WorkflowRepository {
  /**
   * Search for workflows matching a given query. Runs are returned ordered in inverse chronological order, the most
   * recent matching workflow being the first result. It will only consider the latest version of each workflow, and
   * return the latest version of matching workflow in results. It does *not* include graph nodes of each workflow,
   * always an empty graph.
   *
   * @param query Query.
   * @return List of workflow and total number of results.
   */
  def find(query: WorkflowQuery): WorkflowList

  /**
   * Retrieve a specific workflow at its latest version, if it exists.
   *
   * @param id Workflow identifier.
   */
  def get(id: WorkflowId): Option[Workflow]

  /**
   * Retrieve a specific workflow at a specific version, if it exists.
   *
   * @param id      Workflow identifier.
   * @param version Version identifier.
   */
  def get(id: WorkflowId, version: String): Option[Workflow]

  /**
   * Check whether a specific workflow exists.
   *
   * @param id Workflow identifier.
   * @return True if the workflow exists, false otherwise.
   */
  def contains(id: WorkflowId): Boolean

  /**
   * Check whether a specific workflow exists at a specific version.
   *
   * @param id      Workflow identifier.
   * @param version Version identifier.
   * @return True if the workflow exists, false otherwise.
   */
  def contains(id: WorkflowId, version: String): Boolean
}

/**
 * Mutable workflow repository. For now, there is intentionally no method to remove a workflow, because it is
 * not desirable to delete a workflow with runs referencing it.
 *
 * Repositories are *not* required to be thread-safe. Mutating methods might need to be wrapped inside transactions
 * on the application-level. However, repositories should still take care not to leave data in a corrupted state,
 * which can be hard to recover from.
 */
trait MutableWorkflowRepository extends WorkflowRepository {
  /**
   * Save a run. It will either create a new workflow or a new version if there is already one with the same
   * identifier. Workflows are never replaced.
   *
   * @param workflow Workflow to save.
   */
  def save(workflow: Workflow): Unit
}

/**
 * Query to search for workflows. Please note that you have to specify a maximum number of results.
 *
 * @param owner  Only include workflows owner by a given user.
 * @param name   Only include runs whose name matches a given string. Exact interpretation can be implementation-dependant.
 * @param limit  Maximum number of matching workflows to return. Must be in [1,100].
 * @param offset Number of matching workflows to skip.
 */
case class WorkflowQuery(
  owner: Option[String] = None,
  name: Option[String] = None,
  limit: Int = 25,
  offset: Option[Int] = None) {
  require(limit > 0 && limit <= 100, s"Maximum number of workflow must be in [1,100] (got $limit)")
}

/**
 * List of workflows and total number of results.
 *
 * @param results    List of workflows.
 * @param totalCount Total number of results.
 */
case class WorkflowList(results: Seq[Workflow], totalCount: Int)