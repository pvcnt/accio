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

import fr.cnrs.liris.accio.core.statemgr.StateManager

trait Storage {
  def read[T](fn: RepositoryProvider => T): T

  def write[T](fn: MutableRepositoryProvider => T): T
}

trait RepositoryProvider {
  def tasks: TaskRepository

  def runs: RunRepository

  def workflows: WorkflowRepository
}

trait MutableRepositoryProvider {
  def tasks: MutableTaskRepository

  def runs: MutableRunRepository

  def workflows: MutableWorkflowRepository
}

abstract class AbstractStorage(stateManager: StateManager) extends Storage {
  override def read[T](fn: RepositoryProvider => T): T = {
    val provider = new RepositoryProvider {
      override def tasks: TaskRepository = taskRepository

      override def runs: RunRepository = runRepository

      override def workflows: WorkflowRepository = workflowRepository
    }
    fn(provider)
  }

  override def write[T](fn: MutableRepositoryProvider => T): T = {
    val provider = new MutableRepositoryProvider {
      override def tasks: MutableTaskRepository = taskRepository

      override def runs: MutableRunRepository = runRepository

      override def workflows: MutableWorkflowRepository = workflowRepository
    }
    fn(provider)
  }

  protected def taskRepository: MutableTaskRepository

  protected def runRepository: MutableRunRepository

  protected def workflowRepository: MutableWorkflowRepository
}