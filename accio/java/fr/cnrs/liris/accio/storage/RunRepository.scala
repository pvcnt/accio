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

import com.google.common.util.concurrent.Service
import fr.cnrs.liris.accio.api.thrift._

/**
 * Repository giving access to runs.
 */
trait RunRepository extends Service {
  /**
   * Search for runs matching a given query. Runs are returned ordered in inverse chronological order, the most
   * recent matching run being the first result. It does *not* include the result of each node.
   *
   * @param query Query.
   * @return List of runs and total number of results.
   */
  def find(query: RunQuery): RunList

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

/**
 * Mutable run repository.
 *
 * Mutating methods are *not* required to be thread-safe in the sense they will be wrapped inside transactions
 * at the application-level. However, they should still take care not to leave data in a corrupted state,
 * which can be hard to recover from.
 */
trait MutableRunRepository extends RunRepository {
  /**
   * Save a run. It will either create a new run or replace an existing one with the same identifier.
   *
   * @param run Run to save.
   */
  def save(run: Run): Unit

  /**
   * Delete a run, if it exists. It will also delete all associated logs. It does *not* remove child runs, it is up
   * to client code to do this.
   *
   * @param id Run identifier.
   */
  def remove(id: RunId): Unit

  def transactional[T](id: Option[RunId])(fn: Option[Run] => T): T = id match {
    case Some(i) => transactional(i)(fn)
    case None => fn(None)
  }

  def transactional[T](id: RunId)(fn: Option[Run] => T): T

  final def foreach[T](id: RunId)(fn: Run => Unit): Unit = transactional(id)(_.foreach(fn))
}

/**
 * Query to search for runs.
 *
 * @param workflow   Only include runs being instances of a given workflow.
 * @param owner      Only include runs initiated by a given user.
 * @param name       Only include runs whose name matches a given string. Exact interpretation can be implementation-dependant.
 * @param status     Only include runs whose status belong to those specified.
 * @param parent     Only include runs being a child of a given run.
 * @param clonedFrom Only include runs being cloned from of a given run.
 * @param tags       Only include runs having all of specified tags.
 * @param q          Multi-criteria search across workflow, owner, name and tags.
 * @param limit      Maximum number of matching runs to return.
 * @param offset     Number of matching runs to skip.
 */
case class RunQuery(
  workflow: Option[WorkflowId] = None,
  owner: Option[String] = None,
  name: Option[String] = None,
  status: Set[TaskState] = Set.empty,
  parent: Option[RunId] = None,
  clonedFrom: Option[RunId] = None,
  tags: Set[String] = Set.empty,
  q: Option[String] = None,
  limit: Option[Int] = None,
  offset: Option[Int] = None) {

  /**
   * Check whether a given run matches this query.
   *
   * @param run Run.
   * @return True if the run would be included in this query, false otherwise.
   */
  def matches(run: Run): Boolean = {
    if (workflow.isDefined && workflow.get != run.pkg.workflowId) {
      false
    } else if (name.isDefined && !run.name.contains(name.get)) {
      false
    } else if (owner.isDefined && owner.get != run.owner.name) {
      false
    } else if (status.nonEmpty && !status.contains(run.state.status)) {
      false
    } else if (clonedFrom.isDefined && !run.clonedFrom.contains(clonedFrom.get)) {
      false
    } else if (tags.nonEmpty && run.tags.intersect(tags).size < tags.size) {
      false
    } else {
      parent match {
        case Some(parentId) => run.parent.contains(parentId)
        case None => run.parent.isEmpty
      }
    }
  }
}

/**
 * List of runs and total number of results.
 *
 * @param results    List of runs.
 * @param totalCount Total number of results.
 */
case class RunList(results: Seq[Run], totalCount: Int)