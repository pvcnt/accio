/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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
 */
trait RunRepository {
  def find(query: RunQuery): RunList

  def find(query: LogsQuery): Seq[RunLog]

  def save(run: Run): Unit

  def save(logs: Seq[RunLog]): Unit

  def get(id: RunId): Option[Run]

  def exists(id: RunId): Boolean

  def remove(id: RunId): Unit
}

/**
 * Query to search for runs.
 *
 * @param workflow
 * @param cluster
 * @param owner
 * @param environment
 * @param name
 * @param status
 * @param limit
 * @param offset
 */
case class RunQuery(
  workflow: Option[WorkflowId] = None,
  cluster: Option[String] = None,
  owner: Option[String] = None,
  environment: Option[String] = None,
  name: Option[String] = None,
  status: Option[RunStatus] = None,
  limit: Option[Int] = None,
  offset: Option[Int] = None)

/**
 * List of runs and total number of results.
 *
 * @param results    List of runs.
 * @param totalCount Total number of results.
 */
case class RunList(results: Seq[Run], totalCount: Int)

/**
 * Query to search for logs.
 *
 * @param runId
 * @param nodeName
 * @param classifier
 * @param limit
 * @param since
 */
case class LogsQuery(
  runId: RunId,
  nodeName: String,
  classifier: Option[String] = None,
  limit: Option[Int] = None,
  since: Option[Time] = None)