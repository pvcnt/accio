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
import fr.cnrs.liris.common.util.Seqs

/**
 * Exception thrown if a workflow is incorrectly defined.
 *
 * @param message Error message
 * @param cause   Root cause
 */
class IllegalWorkflowException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)

/**
 * Factory for [[Workflow]]s.
 *
 * @param parser       Workflow parser.
 * @param graphFactory Graph factory.
 * @param opRegistry   Operator registry.
 */
final class WorkflowFactory @Inject()(parser: WorkflowParser, graphFactory: GraphFactory, opRegistry: OpRegistry) {
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

    // We create (and validate) the graph associated with the workflow. This is the main source of errors.
    val graph = try {
      graphFactory.create(defn.graph)
    } catch {
      case e: IllegalGraphException => throw new IllegalWorkflowException(e.getMessage)
    }

    // We extract parameters from the ports where they are used.
    val params = getParams(graph)

    // Use owner provided in the definition, or current user otherwise.
    val owner = defn.owner.getOrElse(user)

    Workflow(name = defn.name, graph = graph, owner = owner, params = params)
  }

  /**
   * Extract parameters that are used inside a graph and validae they are correctly used.
   *
   * @param graph Correct graph.
   */
  private def getParams(graph: Graph) = {
    // First we extract all references to parameters with their names and references to ports where they are used.
    val paramUsages = Seqs.index(graph.nodes.flatMap { node =>
      node.inputs.flatMap { case (inputName, input) =>
        input match {
          case ParamInput(paramName) => Seq((paramName, Reference(node.name, inputName)))
          case _ => Seq.empty
        }
      }
    })

    // Then we aggregate these usages into single parameters, and check they are correct.
    paramUsages.map { case (paramName, ports) =>
      // We check param name is valid.
      if (Param.NameRegex.findFirstIn(paramName).isEmpty) {
        throw new IllegalWorkflowException(s"Invalid param name: $paramName (must match ${Param.NamePattern})")
      }

      // We are guaranteed here, from the graph construction, that the node and port names are valid.
      val inputs = ports.map(ref => opRegistry(graph(ref.node).op).defn.inputs.find(_.name == ref.port).get)

      // We check the parameter is homogeneous, i.e., it is used in ports of the same type.
      val dataTypes = inputs.map(_.kind)
      if (dataTypes.size > 1) {
        throw new IllegalWorkflowException(s"Param $paramName is used in heterogeneous input types: ${dataTypes.mkString(", ")}")
      }

      // Parameter is optional only if used only in optional ports.
      val isOptional = inputs.forall(_.isOptional)

      Param(paramName, dataTypes.head, isOptional, ports)
    }.toSet
  }
}