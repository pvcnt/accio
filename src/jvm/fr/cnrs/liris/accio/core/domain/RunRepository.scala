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

package fr.cnrs.liris.accio.core.domain

import com.twitter.util.Time

/**
 * Repository persisting runtime data collected as runs are executed.
 *
 * Repositories are *not* required to be thread-safe. Mutating methods might need to be wrapped inside transactions
 * on the application-level. However, repositories should still take care not to leave data in a corrupted state,
 * which can be hard to recover from.
 */
trait RunRepository extends ReadOnlyRunRepository {
  def save(run: Run): Unit

  def save(logs: Seq[RunLog]): Unit

  def remove(id: RunId): Unit
}

/**
 * Read-only run repository.
 */
trait ReadOnlyRunRepository {
  def find(query: RunQuery): RunList

  def find(query: LogsQuery): Seq[RunLog]

  def get(id: RunId): Option[Run]

  def get(cacheKey: CacheKey): Option[OpResult]

  def contains(id: RunId): Boolean

  def contains(cacheKey: CacheKey): Boolean
}

/**
 * Query to search for runs. Please note that you have to specify a maximum number of results.
 *
 * @param workflow Only include runs being instances of a given workflow.
 * @param owner    Only include runs initiated by a given user.
 * @param name     Only include runs whose name matches a given string. Exact interpretation can be implementation-dependant.
 * @param status   Only include runs whose status belong to those specified.
 * @param limit    Maximum number of matching runs to return. Must be in [1,100].
 * @param offset   Number of matching runs to skip.
 */
case class RunQuery(
  workflow: Option[WorkflowId] = None,
  owner: Option[String] = None,
  name: Option[String] = None,
  status: Set[RunStatus] = Set.empty,
  limit: Int = 50,
  offset: Option[Int] = None) {
  require(limit > 0 && limit <= 100, s"Maximum number of runs must be in [1,100] (got $limit)")
}

/**
 * List of runs and total number of results.
 *
 * @param results    List of runs.
 * @param totalCount Total number of results.
 */
case class RunList(results: Seq[Run], totalCount: Int)

/**
 * Query to search for logs. It is only possible to search for logs issued by a specific task (i.e., a run/node
 * combination). Please note that you have to specify a maximum number of results.
 *
 * @param runId      Run for which to retrieve the logs.
 * @param nodeName   Node for which to retrieve the logs.
 * @param classifier Only include logs with this classifier.
 * @param limit      Maximum number of matching logs to return.
 * @param since      Only include logs older than a given instant.
 */
case class LogsQuery(
  runId: RunId,
  nodeName: String,
  classifier: Option[String] = None,
  limit: Option[Int] = None,
  since: Option[Time] = None)