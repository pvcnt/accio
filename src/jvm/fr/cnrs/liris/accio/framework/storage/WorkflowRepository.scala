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

package fr.cnrs.liris.accio.framework.storage

import com.google.common.util.concurrent.Service
import com.twitter.util.Future
import fr.cnrs.liris.accio.framework.api.thrift.{Workflow, WorkflowId}

/**
 * Repository giving access to workflows.
 */
trait WorkflowRepository extends Service {
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
}

/**
 * Mutable workflow repository.
 *
 * For now, there is intentionally no method to remove a workflow, because it is not desirable to delete a workflow
 * with runs referencing it.
 *
 * Mutating methods are *not* required to be thread-safe in the sense they will be wrapped inside transactions
 * at the application-level. However, they should still take care not to leave data in a corrupted state,
 * which can be hard to recover from.
 */
trait MutableWorkflowRepository extends WorkflowRepository {
  /**
   * Save a workflow. It will either create a new workflow or a new version if there is already one with the same
   * identifier. Workflows are never replaced.
   *
   * @param workflow Workflow to save.
   */
  def save(workflow: Workflow): Unit
}

/**
 * Query to search for workflows.
 *
 * @param owner  Only include workflows owner by a given user.
 * @param name   Only include runs whose name matches a given string. Exact interpretation can be implementation-dependant.
 * @param q      Multi-criteria search among owner and name.
 * @param limit  Maximum number of matching workflows to return.
 * @param offset Number of matching workflows to skip.
 */
case class WorkflowQuery(
  owner: Option[String] = None,
  name: Option[String] = None,
  q: Option[String] = None,
  limit: Option[Int] = None,
  offset: Option[Int] = None) {

  /**
   * Check whether a given log matches this query.
   *
   * @param workflow Workflow.
   * @return True if the workflow would be included in this query, false otherwise.
   */
  def matches(workflow: Workflow): Boolean = {
    (name.isEmpty || workflow.name.contains(name.get)) &&
      (owner.isEmpty || workflow.owner.map(_.name).contains(owner.get))
  }
}

/**
 * List of workflows and total number of results.
 *
 * @param results    List of workflows.
 * @param totalCount Total number of results.
 */
case class WorkflowList(results: Seq[Workflow], totalCount: Int)