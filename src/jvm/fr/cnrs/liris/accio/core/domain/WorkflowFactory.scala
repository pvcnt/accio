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

import java.util.Objects

import com.google.inject.Inject
import fr.cnrs.liris.common.util.{HashUtils, Seqs}

/**
 * Factory for [[Workflow]].
 *
 * @param graphFactory Graph factory.
 * @param opRegistry   Operator registry.
 */
final class WorkflowFactory @Inject()(graphFactory: GraphFactory, opRegistry: OpRegistry) {
  @throws[InvalidWorkflowException]
  def create(spec: WorkflowSpec, user: User): Workflow = {
    val graph = getGraph(spec)
    val params = getParams(graph, spec.params.toSet)
    val owner = spec.owner.getOrElse(user)
    val version = spec.version.getOrElse(getVersion(spec.id, spec.name, spec.description, owner, spec.graph, params))
    Workflow(
      id = spec.id,
      version = version,
      createdAt = System.currentTimeMillis(),
      name = spec.name,
      description = spec.description,
      owner = owner,
      graph = spec.graph,
      params = params)
  }

  /**
   * Create (and validate) the graph associated with the workflow.
   *
   * @param spec Workflow specification.
   */
  private def getGraph(spec: WorkflowSpec) = {
    try {
      graphFactory.create(spec.graph)
    } catch {
      case e: InvalidGraphException => throw new InvalidWorkflowException(e.getMessage, e.getCause)
    }
  }

  private def getVersion(id: WorkflowId, name: Option[String], description: Option[String], owner: User, graph: GraphDef, params: Set[ArgDef]) = {
    HashUtils.sha1(Objects.hash(id.value, name.getOrElse(""), description.getOrElse(""), owner, graph, params).toString)
  }

  /**
   * Validate workflow parameters are correctly used.
   *
   * @param graph
   * @param params
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

    // Then we aggregate these usages into single parameters, and check they are correct.
    paramUsages.map { case (paramName, usages) =>
      // We check param name is valid.
      if (Utils.ArgRegex.findFirstIn(paramName).isEmpty) {
        throw new InvalidWorkflowException(s"Invalid param name: $paramName")
      }

      // We check the parameter is homogeneous, i.e., it is used in ports of the same type.
      val dataTypes = usages.map(_.argDef).map(_.kind)
      if (dataTypes.size > 1) {
        throw new InvalidWorkflowException(s"Param $paramName is used in heterogeneous input types: ${dataTypes.mkString(", ")}")
      }

      // Parameter is optional only if used only in optional ports or if a default value is available each time.
      val isOptional = usages.forall(usage => usage.argDef.isOptional || usage.argDef.defaultValue.isDefined)

      params.find(_.name == paramName) match {
        case Some(argDef) =>
          // TODO: validate def.
          argDef
        case None => ArgDef(paramName, None, dataTypes.head, isOptional)
      }
    }.toSet
  }
}