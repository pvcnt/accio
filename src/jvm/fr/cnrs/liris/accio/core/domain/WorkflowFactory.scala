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

import fr.cnrs.liris.common.util.{HashUtils, Seqs}

/**
 * Factory for [[Workflow]].
 *
 * @param graphFactory Graph factory.
 * @param opRegistry   Operator registry.
 */
final class WorkflowFactory(graphFactory: GraphFactory, opRegistry: OpRegistry) {
  /**
   * Create a workflow from a workflow template.
   *
   * @param template Workflow template.
   * @param user     User creating the workflow.
   * @throws InvalidWorkflowException
   */
  @throws[InvalidWorkflowException]
  def create(template: WorkflowTemplate, user: User): Workflow = {
    val graph = getGraph(template)
    val params = getParams(graph, template.params.toSet)
    val owner = template.owner.getOrElse(user)
    val version = template.version.getOrElse(defaultVersion(template.id, template.name, owner, template.graph, params))
    Workflow(
      id = template.id,
      version = version,
      createdAt = System.currentTimeMillis(),
      name = template.name,
      owner = owner,
      graph = template.graph,
      params = params)
  }

  /**
   * Create (and validate) the graph associated with the workflow.
   *
   * @param template Workflow template.
   */
  private def getGraph(template: WorkflowTemplate) = {
    try {
      graphFactory.create(template.graph)
    } catch {
      case e: InvalidGraphException => throw new InvalidWorkflowException(e.getMessage, e.getCause)
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