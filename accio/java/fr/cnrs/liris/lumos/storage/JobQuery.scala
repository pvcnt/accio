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

import fr.cnrs.liris.lumos.domain.{ExecStatus, Job, LabelSelector}

/**
 * Query to search for jobs.
 *
 * @param owner  Only include jobs owned by a given user.
 * @param state  Only include jobs whose state belong to those specified.
 * @param labels Only include jobs whose labels match the given selector.
 */
case class JobQuery(
  owner: Option[String] = None,
  state: Set[ExecStatus.State] = Set.empty,
  labels: Option[LabelSelector] = None) {

  /**
   * Check whether a given job matches this query.
   *
   * @param job Job.
   */
  private[storage] def matches(job: Job): Boolean = {
    (state.isEmpty || state.contains(job.status.state)) &&
      owner.forall(job.owner.contains) &&
      labels.forall(_.matches(job.labels))
  }
}