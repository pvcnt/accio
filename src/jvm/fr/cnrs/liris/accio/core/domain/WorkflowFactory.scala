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

package fr.cnrs.liris.accio.core.domain

import java.util.Objects

import com.google.inject.Inject
import fr.cnrs.liris.common.util.{HashUtils, Seqs}

/**
 * Exception thrown if a workflow definition is invalid.
 *
 * @param message Message explaining the error.
 * @param cause   Cause exception.
 */
class InvalidWorkflowDefException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)

/**
 * Factory for [[Workflow]].
 *
 * @param graphFactory Graph factory.
 * @param opRegistry   Operator registry.
 */
final class WorkflowFactory @Inject()(graphFactory: GraphFactory, opRegistry: OpRegistry) {
  /**
   * Create a workflow from a workflow definition.
   *
   * @param defn Workflow definition.
   * @param user User creating the workflow.
   * @throws InvalidWorkflowDefException If the workflow definition is invalid.
   */
  @throws[InvalidWorkflowDefException]
  def create(defn: WorkflowDef, user: User): Workflow = {
    val graph = getGraph(defn)
    val params = getParams(graph, defn.params.toSet)
    val owner = defn.owner.getOrElse(user)
    val version = defn.version.getOrElse(defaultVersion(defn.id, defn.name, owner, defn.graph, params))
    Workflow(
      id = defn.id,
      version = version,
      isActive = true,
      createdAt = System.currentTimeMillis(),
      name = defn.name,
      owner = owner,
      graph = defn.graph,
      params = params)
  }

  /**
   * Create (and validate) the graph associated with the workflow.
   *
   * @param defn Workflow definition.
   */
  private def getGraph(defn: WorkflowDef) = {
    try {
      graphFactory.create(defn.graph)
    } catch {
      case e: InvalidGraphException => throw new InvalidWorkflowDefException(e.getMessage, e.getCause)
    }
  }

  /**
   * Compute a version key by creating a hash workflow fields that identify whether to workflows are identical.
   *
   * @param id     Workflow identifier.
   * @param name   Name.
   * @param owner  Owner.
   * @param graph  Graph definition.
   * @param params Workflow parameters.
   */
  private def defaultVersion(id: WorkflowId, name: Option[String], owner: User, graph: GraphDef, params: Set[ArgDef]) = {
    HashUtils.sha1(Objects.hash(id.value, name.getOrElse(""), owner, graph, params).toString)
  }

  /**
   * Validate workflow parameters are correctly used.
   *
   * @param graph  Graph definition.
   * @param params Workflow parameters.
   */
  private def getParams(graph: Graph, params: Set[ArgDef]) = {
    case class ParamUsage(ref: Reference, argDef: ArgDef)

    // First we extract all references to parameters with their names and references to ports where they are used.
    val paramUsages = Seqs.index(graph.nodes.flatMap { node =>
      node.inputs.flatMap {
        case (inputName, ParamInput(paramName)) =>
          val argDef = opRegistry(graph(node.name).op).inputs.find(_.name == inputName).get
          Some(paramName -> ParamUsage(Reference(node.name, inputName), argDef))
        case _ => None
      }
    })

    // Check for undeclared params usage.
    val undeclaredParams = paramUsages.keySet.diff(params.map(_.name))
    if (undeclaredParams.nonEmpty) {
      throw new InvalidWorkflowDefException(s"Some params are used but not declared: ${undeclaredParams.toSeq.sorted.mkString(", ")}")
    }

    params.map { argDef =>
      // Check the parameter name is valid.
      if (Utils.ArgRegex.findFirstIn(argDef.name).isEmpty) {
        throw new InvalidWorkflowDefException(s"Illegal param name: ${argDef.name}")
      }

      // Check the parameter is homogeneous, i.e., it is used in ports of the same type, and consistent with the type
      // of those ports.
      val usages = paramUsages.getOrElse(argDef.name, Set.empty)
      val dataTypes = usages.map(_.argDef).map(_.kind)
      if (dataTypes.size > 1) {
        throw new InvalidWorkflowDefException(s"Param ${argDef.name} is used with heterogeneous types: " +
          dataTypes.map(Utils.toString).toSeq.sorted.mkString(", "))
      }
      if (dataTypes.head != argDef.kind) {
        throw new InvalidWorkflowDefException(s"Param ${argDef.name} declared as ${Utils.toString(argDef.kind)} is " +
          s"used as ${Utils.toString(dataTypes.head)}")
      }

      // Parameter is optional if either:
      // (1) a default value is defined, or
      // (2) it is used only in optional ports or if a default value is available each time.
      val isOptional = argDef.defaultValue.isDefined || usages.forall(usage => usage.argDef.isOptional || usage.argDef.defaultValue.isDefined)

      argDef.copy(isOptional = isOptional)
    }
  }
}