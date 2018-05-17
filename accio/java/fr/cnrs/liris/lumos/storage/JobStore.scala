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

package fr.cnrs.liris.lumos.storage

import com.twitter.util.{Closable, Future}
import fr.cnrs.liris.lumos.domain.{Job, JobList, Status}

/**
 * Store providing access to jobs.
 */
trait JobStore extends Closable {
  /**
   * Search for jobs matching a given query. Jobs are returned ordered in inverse chronological
   * order, the most recent matching job being the first result.
   *
   * @param query  Query.
   * @param limit  Maximum number of jobs to retrieve.
   * @param offset Number of matching jobs to skip.
   */
  def list(query: JobQuery = JobQuery(), limit: Option[Int] = None, offset: Option[Int] = None): Future[JobList]

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
  def create(job: Job): Future[Status]

  def replace(job: Job): Future[Status]

  def delete(name: String): Future[Status]

  def startUp(): Future[Unit]
}