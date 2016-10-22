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

package fr.cnrs.liris.accio.core.framework

import com.google.inject.Inject

/**
 * Exception thrown if a workflow is incorrectly defined.
 *
 * @param message Error message
 * @param cause   Root cause
 */
class IllegalWorkflowException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)

/**
 * Parser for [[WorkflowDef]]s.
 */
trait WorkflowParser {
  /**
   * Parse a file into a workflow definition.
   *
   * @param uri URI to a workflow definition.
   */
  def parse(uri: String): WorkflowDef

  /**
   * Check whether a given URI could be read as a workflow definition.
   *
   * @param uri URI to (possibly) a workflow definition.
   * @return True if it could be read as a workflow definition, false otherwise.
   */
  def canRead(uri: String): Boolean
}

/**
 * Factory for [[Workflow]]s.
 *
 * @param parser       Workflow parser.
 * @param graphFactory Graph factory.
 */
final class WorkflowFactory @Inject()(parser: WorkflowParser, graphFactory: GraphFactory) {
  /**
   * Create a workflow.
   *
   * @param uri  URI to a workflow definition.
   * @param user User creating this workflow.
   * @throws IllegalWorkflowException If the workflow definition is invalid.
   */
  @throws[IllegalWorkflowException]
  def create(uri: String, user: User): Workflow = {
    val defn = parser.parse(uri)

    // Create (and validate) the graph associated with the workflow. This is the main source of errors.
    val graph = try {
      graphFactory.create(defn.graph)
    } catch {
      case e: IllegalGraphException => throw new IllegalWorkflowException(e.getMessage)
    }

    // Use owner provided in the definition, or current user otherwise.
    val owner = defn.owner.getOrElse(user)

    Workflow(name = defn.name, graph = graph, owner = owner)
  }
}