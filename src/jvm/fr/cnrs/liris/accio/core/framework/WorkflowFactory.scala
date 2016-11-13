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

import java.nio.file.Paths

import com.google.inject.Inject
import fr.cnrs.liris.common.util.{FileUtils, Seqs}

import scala.util.control.NonFatal

/**
 * Exception thrown if a workflow is incorrectly defined.
 *
 * @param uri     URI to the workflow definition.
 * @param message Error message.
 * @param cause   Root cause, if any.
 */
class IllegalWorkflowException(uri: String, message: String, cause: Throwable = null) extends RuntimeException(message, cause)

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
    val defn = try {
      parser.parse(uri)
    } catch {
      case NonFatal(e) => throw new IllegalWorkflowException(uri, "JSON syntax error", e)
    }

    // We create (and validate) the graph associated with the workflow. This is the main source of errors.
    val graph = try {
      graphFactory.create(defn.graph)
    } catch {
      case e: IllegalGraphException => throw new IllegalWorkflowException(uri, e.getMessage, e.getCause)
    }

    val params = getParams(uri, graph)
    val owner = defn.owner.getOrElse(user)
    val name = defn.name.getOrElse(getDefaultName(uri))

    Workflow(name = name, graph = graph, owner = owner, params = params)
  }

  /**
   * Extract parameters that are used inside a graph and validate they are correctly used.
   *
   * @param uri  URI to the workflow definition.
   * @param graph Correct graph.
   */
  private def getParams(uri: String, graph: Graph) = {
    case class ParamUsage(ref: Reference, defaultValue: Option[Any], inputDef: InputArgDef)

    // First we extract all references to parameters with their names and references to ports where they are used.
    val paramUsages = Seqs.index(graph.nodes.flatMap { node =>
      node.inputs.flatMap {
        case (inputName, ParamInput(paramName, defaultValue)) =>
          val argDef = opRegistry(graph(node.name).op).defn.inputs.find(_.name == inputName).get
          Some(paramName -> ParamUsage(Reference(node.name, inputName), defaultValue, argDef))
        case _ => None
      }
    })

    // Then we aggregate these usages into single parameters, and check they are correct.
    paramUsages.map { case (paramName, usages) =>
      // We check param name is valid.
      if (Param.NameRegex.findFirstIn(paramName).isEmpty) {
        throw new IllegalWorkflowException(uri, s"Invalid param name: $paramName (must match ${Param.NamePattern})")
      }

      // We check the parameter is homogeneous, i.e., it is used in ports of the same type.
      val dataTypes = usages.map(_.inputDef).map(_.kind)
      if (dataTypes.size > 1) {
        throw new IllegalWorkflowException(uri, s"Param $paramName is used in heterogeneous input types: ${dataTypes.mkString(", ")}")
      }

      // Parameter is optional only if used only in optional ports or if a default value is available each time.
      val isOptional = usages.forall(usage => usage.defaultValue.isDefined || usage.inputDef.isOptional)

      Param(paramName, dataTypes.head, isOptional, usages.map(_.ref))
    }.toSet
  }

  /**
   * Return the default name for a workflow, which is inferred from the filename portion in its uri.
   *
   * @param uri URI to a workflow definition.
   */
  private def getDefaultName(uri: String) =
    FileUtils.removeExtension(Paths.get(FileUtils.replaceHome(uri)).getFileName.toString)
}