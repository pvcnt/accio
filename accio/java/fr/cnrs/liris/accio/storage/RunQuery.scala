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

import fr.cnrs.liris.accio.api.thrift.{Run, TaskState}

/**
 * Query to search for runs.
 *
 * @param workflow   Only include runs being instances of a given workflow.
 * @param owner      Only include runs initiated by a given user.
 * @param name       Only include runs whose name includes a given string.
 * @param status     Only include runs whose status belong to those specified.
 * @param parent     Only include runs being a child of a given run.
 * @param clonedFrom Only include runs being cloned from of a given run.
 * @param tags       Only include runs having all of specified tags.
 * @param q          Multi-criteria search across workflow, owner, name and tags. If specified,
 *                   fields `workflow`, `owner`, `name` and `tags` are ignored.
 * @param limit      Maximum number of matching runs to return.
 * @param offset     Number of matching runs to skip.
 */
case class RunQuery(
  workflow: Option[String] = None,
  owner: Option[String] = None,
  name: Option[String] = None,
  status: Set[TaskState] = Set.empty,
  parent: Option[String] = None,
  clonedFrom: Option[String] = None,
  tags: Set[String] = Set.empty,
  q: Option[String] = None,
  limit: Option[Int] = None,
  offset: Option[Int] = None) {

  /**
   * Check whether a given run matches this query.
   *
   * @param run Run.
   */
  def matches(run: Run): Boolean = {
    q match {
      //TODO: tokenize string.
      case Some(str) =>
        str.split(' ').map(_.trim).forall { s =>
          name.contains(s) ||
            run.owner.exists(_.name == s) ||
            run.pkg.workflowId == s ||
            run.tags.contains(s)
        }
      case None =>
        if (workflow.isDefined && workflow.get != run.pkg.workflowId) {
          false
        } else if (name.isDefined && !run.name.contains(name.get)) {
          false
        } else if (owner.isDefined && !run.owner.exists(_.name == owner.get)) {
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
}
