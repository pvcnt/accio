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

/**
 * Repository persisting workflows.
 */
trait WorkflowRepository {
  def find(query: WorkflowQuery): WorkflowList

  def save(workflow: Workflow): Unit

  def get(id: WorkflowId): Option[Workflow]

  def get(id: WorkflowId, version: String): Option[Workflow]

  def exists(id: WorkflowId): Boolean

  def exists(id: WorkflowId, version: String): Boolean
}

case class WorkflowQuery(
  owner: Option[String] = None,
  name: Option[String] = None,
  limit: Option[Int] = None,
  offset: Option[Int] = None)

case class WorkflowList(results: Seq[Workflow], totalCount: Int)