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

import com.twitter.util.Future
import fr.cnrs.liris.accio.api.thrift._

/**
 * Store providing access to jobs.
 */
trait JobStore {
  /**
   * Search for jobs matching a given query. Jobs are returned ordered in inverse chronological
   * order, the most recent matching job being the first result.
   *
   * @param query  Query.
   * @param limit  Maximum number of jobs to retrieve.
   * @param offset Number of matching jobs to skip.
   */
  def list(
    query: JobStore.Query = JobStore.Query(),
    limit: Option[Int] = None,
    offset: Option[Int] = None): Future[ResultList[Job]]

  /**
   * Retrieve a specific job, if it exists.
   *
   * @param name Job identifier.
   */
  def get(name: String): Future[Option[Job]]

  /**
   * Save a job.
   *
   * @param job Job to save.
   */
  def create(job: Job): Future[Boolean]

  def replace(job: Job): Future[Boolean]

  /**
   * Delete a job, if it exists. It does *not* remove child jobs, it is up to client code to
   * do this.
   *
   * @param name Job name.
   */
  def delete(name: String): Future[Boolean]
}

object JobStore {

  /**
   * Query to search for jobs.
   *
   * @param author     Only include jobs initiated by a given user.
   * @param title      Only include jobs whose title includes a given string.
   * @param state      Only include jobs whose state belong to those specified.
   * @param parent     If a non-empty string is given, only include jobs being a child of the given
   *                   job. If an empty string is given , only include jobs having no parent
   *                   (i.e., not being a child of any other job).
   * @param tags       Only include jobs having all of specified tags.
   * @param q          Multi-criteria search. If specified, fields `author`, `title` and `tags`
   *                   are ignored.
   */
  case class Query(
    author: Option[String] = None,
    title: Option[String] = None,
    state: Option[Set[ExecState]] = None,
    parent: Option[String] = None,
    tags: Set[String] = Set.empty,
    q: Option[String] = None) {

    /**
     * Check whether a given job matches this query.
     *
     * @param job Job.
     */
    private[storage] def matches(job: Job): Boolean = {
      val res1 = q match {
        //TODO: tokenize string.
        case Some(str) =>
          str.split(' ').map(_.trim).forall { s =>
            title.exists(_.contains(s)) ||
              job.author.exists(_.name == s) ||
              job.tags.contains(s)
          }
        case None =>
          author.forall(s => job.author.exists(_.name == s)) &&
            title.forall(s => job.title.exists(_.contains(s))) &&
            tags.forall(s => job.tags.contains(s))
      }

      res1 &&
        state.forall(_.contains(job.status.state)) &&
        parent.forall(parent => parent.isEmpty && job.parent.isEmpty || job.parent.contains(parent))
    }
  }


}