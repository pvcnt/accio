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
 *
 * This repository actually handles three aggregates: [[Run]]s, [[Task]]s and [[TaskLog]]. Although runs are the root
 * for tasks and logs, these three kinds of objects can be saved and retrieved independently. However, only runs
 * can be deleted, which triggers the deletion of associated tasks and logs.
 */
trait RunRepository {
  /**
   * Search for runs by some criteria.
   *
   * @param query Runs query.
   * @return List of runs and total number of matching results.
   */
  def find(query: RunQuery): RunList

  /**
   * Search for tasks by some criteria.
   *
   * @param query Tasks query.
   * @return List of tasks and total number of matching results.
   */
  def find(query: TaskQuery): TaskList

  /**
   * Search for logs by some criteria.
   *
   * @param query Logs query.
   * @return List of logs.
   */
  def find(query: LogsQuery): Seq[TaskLog]

  /**
   * Save a run. It can either create a new run or update an existing one (which will be overwritten).
   * This method should be thread-safe.
   *
   * @param run Run to save.
   */
  def save(run: Run): Unit

  /**
   * Save a task. It can either create a new task or update an existing one (which will be overwritten).
   * This method should be thread-safe.
   *
   * @param task Task to save.
   */
  def save(task: Task): Unit

  /**
   * Save a list of logs. Logs are append-only.
   *
   * @param logs Logs to save.
   */
  def save(logs: Seq[TaskLog]): Unit

  /**
   * Return a specific run, if it exists.
   *
   * @param id Run identifier.
   */
  def get(id: RunId): Option[Run]

  def get(id: TaskId): Option[Task]

  /**
   * Check whether a specific run exists.
   *
   * @param id Run identifier.
   */
  def exists(id: RunId): Boolean

  /**
   * Check whether a specific task exists.
   *
   * @param id Task identifier.
   */
  def exists(id: TaskId): Boolean

  /**
   * Delete a run. It also deletes associated tasks and logs.
   *
   * @param id Run identifier.
   */
  def delete(id: RunId): Unit
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
 * Query to search for tasks.
 *
 * @param runId
 * @param status
 * @param limit
 * @param offset
 */
case class TaskQuery(
  runId: Option[RunId] = None,
  status: Option[TaskStatus] = None,
  limit: Option[Int] = None,
  offset: Option[Int] = None)

/**
 * List of tasks and total number of results.
 *
 * @param results    List of tasks.
 * @param totalCount Total number of results.
 */
case class TaskList(results: Seq[Task], totalCount: Int)

/**
 * Query to search for logs.
 *
 * @param taskId
 * @param classifier
 * @param limit
 * @param since
 */
case class LogsQuery(
  taskId: TaskId,
  classifier: Option[String] = None,
  limit: Option[Int] = None,
  since: Option[Time] = None)