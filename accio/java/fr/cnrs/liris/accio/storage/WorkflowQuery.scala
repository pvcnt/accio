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

import fr.cnrs.liris.accio.api.thrift.Workflow

/**
 * Query to search for workflows.
 *
 * @param owner  Only include workflows owner by a given user.
 * @param name   Only include runs whose name includes a given string.
 * @param q      Multi-criteria search among owner and name. If specified, fields `owner` and
 *               `name` are ignored.
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
   */
  def matches(workflow: Workflow): Boolean = {
    q match {
      case Some(str) =>
        str.split(' ').map(_.trim).forall { s =>
          name.contains(s) || workflow.owner.map(_.name).contains(s)
        }
      case None =>
        name.forall(workflow.name.contains) &&
          owner.forall(owner => workflow.owner.map(_.name).contains(owner))
    }
  }
}
