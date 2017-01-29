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

import fr.cnrs.liris.accio.core.domain.{Workflow, WorkflowId}

/**
 * A workflow repository doing nothing.
 */
class NullWorkflowRepository extends MutableWorkflowRepository {
  override def save(workflow: Workflow): Unit = {}

  override def find(query: WorkflowQuery): WorkflowList = WorkflowList(Seq.empty, 0)

  override def get(id: WorkflowId): Option[Workflow] = None

  override def get(id: WorkflowId, version: String): Option[Workflow] = None

  override def contains(id: WorkflowId): Boolean = false

  override def contains(id: WorkflowId, version: String): Boolean = false
}

/**
 * Singleton of [[NullWorkflowRepository]].
 */
object NullWorkflowRepository extends NullWorkflowRepository