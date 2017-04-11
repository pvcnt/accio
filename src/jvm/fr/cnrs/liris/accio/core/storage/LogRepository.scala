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

import com.google.common.util.concurrent.Service
import com.twitter.util.{Future, Time}
import fr.cnrs.liris.accio.core.api._

/**
 * Repository giving access to run logs.
 */
trait LogRepository extends Service {
  /**
   * Search for logs matching a given query. Logs are returned ordered in chronological order, the oldest matching
   * log being the first result.
   *
   * @param query Query.
   * @return List of logs.
   */
  def find(query: LogsQuery): Seq[RunLog]
}

/**
 * Mutable log repository.
 *
 * Mutating methods are *not* required to be thread-safe in the sense they will be wrapped inside transactions
 * at the application-level. However, they should still take care not to leave data in a corrupted state,
 * which can be hard to recover from.
 */
trait MutableLogRepository extends LogRepository {
  /**
   * Save some logs. Since they are small objects, they can be saved in a batch (details are implementation-dependant).
   * Logs are append-only.
   *
   * @param logs Logs to save.
   */
  def save(logs: Seq[RunLog]): Unit

  /**
   * Delete all logs associated with a given run.
   *
   * @param id Run identifier.
   */
  def remove(id: RunId): Unit
}

/**
 * Query to search for logs. It is only possible to search for logs issued by a specific task (i.e., a run/node
 * combination).
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
  since: Option[Time] = None) {

  /**
   * Check whether a given log matches this query.
   *
   * @param log Log.
   * @return True if the log would be included in this query, false otherwise.
   */
  def matches(log: RunLog): Boolean = {
    log.runId == runId &&
      log.nodeName == nodeName &&
      (classifier.isEmpty || log.classifier == classifier.get) &&
      (since.isEmpty || log.createdAt > since.get.inMillis)
  }
}