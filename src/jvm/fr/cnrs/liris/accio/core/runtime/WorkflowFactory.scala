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

package fr.cnrs.liris.accio.core.runtime

import java.util.Objects

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.common.util.{HashUtils, Seqs}

import scala.collection.mutable

/**
 * Factory for [[Workflow]].
 *
 * @param graphFactory Graph factory.
 * @param opRegistry   Operator registry.
 */
final class WorkflowFactory @Inject()(graphFactory: GraphFactory, opRegistry: OpRegistry) extends BaseFactory {
  /**
   * Create a workflow from a workflow specification.
   *
   * @param spec     Workflow specification.
   * @param user     User creating the workflow.
   * @param warnings Mutable list collecting warnings.
   * @throws InvalidSpecException If the workflow specification is invalid.
   */
  @throws[InvalidSpecException]
  def create(spec: WorkflowSpec, user: User, warnings: mutable.Set[InvalidSpecMessage] = mutable.Set.empty[InvalidSpecMessage]): Workflow = {
    val graph = graphFactory.create(spec.graph, warnings)
    val params = getParams(graph, spec.params.toSet, warnings)
    val owner = spec.owner.getOrElse(user)
    val version = spec.version.getOrElse(defaultVersion(spec.id, spec.name, owner, spec.graph, params))
    Workflow(
      id = spec.id,
      version = version,
      isActive = true,
      createdAt = System.currentTimeMillis(),
      name = spec.name,
      owner = owner,
      graph = spec.graph,
      params = params)
  }

  /**
   * Validate that a workflow specification would create a valid workflow. It does not throw any exception if
   * the specification is invalid, errors are returned.
   *
   * @param spec Workflow specification.
   */
  def validate(spec: WorkflowSpec): ValidationResult = {
    doValidate(warnings => create(spec, User("dummy user"), warnings))
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
   * @param graph    Graph definition.
   * @param params   Workflow parameters.
   * @param warnings Mutable list collecting warnings.
   */
  private def getParams(graph: Graph, params: Set[ArgDef], warnings: mutable.Set[InvalidSpecMessage]) = {
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
      throw newError("Param is not declared", undeclaredParams.map(paramName => s"params.$paramName"), warnings)
    }

    params.map { argDef =>
      // Check the parameter name is valid.
      if (Utils.ArgRegex.findFirstIn(argDef.name).isEmpty) {
        throw newError(s"Invalid param name: ${argDef.name}", warnings)
      }

      // Check the parameter is homogeneous, i.e., it is used in ports of the same type, and consistent with the type
      // of those ports.
      val usages = paramUsages.getOrElse(argDef.name, Set.empty)
      val dataTypes = usages.map(_.argDef).map(_.kind)
      if (dataTypes.size > 1) {
        val message = s"Param is used with heterogeneous types: " + dataTypes.map(Utils.toString).toSeq.sorted.mkString(", ")
        throw newError(message, s"params.${argDef.name}", warnings)
      }
      if (dataTypes.head != argDef.kind) {
        val message = s"Param declared as ${Utils.toString(argDef.kind)} is used as ${Utils.toString(dataTypes.head)}"
        throw newError(message, s"params.${argDef.name}", warnings)
      }

      // Parameter is optional if either:
      // (1) a default value is defined, or
      // (2) it is used only in optional ports or if a default value is available each time.
      val isOptional = argDef.defaultValue.isDefined || usages.forall(usage => usage.argDef.isOptional || usage.argDef.defaultValue.isDefined)

      argDef.copy(isOptional = isOptional)
    }
  }
}