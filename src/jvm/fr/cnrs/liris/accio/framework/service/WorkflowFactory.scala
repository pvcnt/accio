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

package fr.cnrs.liris.accio.framework.service

import java.util.Objects

import com.google.inject.Inject
import fr.cnrs.liris.accio.framework.api
import fr.cnrs.liris.accio.framework.api.thrift._
import fr.cnrs.liris.accio.framework.api.{Input, Utils}
import fr.cnrs.liris.common.util.{HashUtils, Seqs}
import fr.cnrs.liris.dal.core.api._

import scala.collection.mutable

/**
 * Factory for [[Workflow]].
 *
 * @param graphFactory   Graph factory.
 * @param opRegistry     Operator registry.
 * @param valueValidator Value validator.
 */
final class WorkflowFactory @Inject()(
  graphFactory: GraphFactory,
  opRegistry: OpRegistry,
  valueValidator: ValueValidator)
  extends BaseFactory {

  /**
   * Create a workflow from a workflow specification.
   *
   * @param spec     Workflow specification.
   * @param user     User creating the workflow.
   * @param warnings Mutable list collecting warnings.
   * @throws InvalidSpecException If the workflow specification is invalid.
   */
  @throws[InvalidSpecException]
  def create(spec: Workflow, user: User, warnings: mutable.Set[InvalidSpecMessage] = mutable.Set.empty[InvalidSpecMessage]): Workflow = {
    val graph = graphFactory.create(spec.graph, warnings)
    val params = getParams(graph, spec.params.toSet, warnings)
    val owner = spec.owner.getOrElse(user)
    val version = spec.version.getOrElse(defaultVersion(spec.id, spec.name, owner, spec.graph, params))
    if (Utils.WorkflowRegex.findFirstIn(spec.id.value).isEmpty) {
      throw newError(s"Invalid workflow identifier: ${spec.id.value} (should match ${Utils.WorkflowPattern})", "id", warnings)
    }
    Workflow(
      id = spec.id,
      version = Some(version),
      isActive = true,
      createdAt = Some(System.currentTimeMillis()),
      name = spec.name,
      owner = Some(owner),
      graph = spec.graph,
      params = params)
  }

  /**
   * Validate that a workflow specification would create a valid workflow. It does not throw any exception if
   * the specification is invalid, errors are returned.
   *
   * @param spec Workflow specification.
   */
  def validate(spec: Workflow): ValidationResult = {
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
  private def defaultVersion(id: WorkflowId, name: Option[String], owner: User, graph: Graph, params: Set[ArgDef]) = {
    HashUtils.sha1(Objects.hash(id.value, name.getOrElse(""), owner, graph, params).toString)
  }

  /**
   * Validate workflow parameters are correctly used.
   *
   * @param graph    Graph definition.
   * @param params   Workflow parameters.
   * @param warnings Mutable list collecting warnings.
   */
  private def getParams(graph: api.Graph, params: Set[ArgDef], warnings: mutable.Set[InvalidSpecMessage]) = {
    case class ParamUsage(ref: Reference, argDef: ArgDef)

    // First we extract all references to parameters with their names and references to ports where they are used.
    val paramUsages = Seqs.index(graph.nodes.flatMap { node =>
      node.inputs.flatMap {
        case (inputName, Input.Param(paramName)) =>
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
        throw newError(s"Invalid param name: ${argDef.name} (should match ${Utils.ArgPattern})", warnings)
      }

      // Check the parameter is homogeneous, i.e., it is used in ports of the same type, and consistent with the type
      // of those ports.
      val usages = paramUsages.getOrElse(argDef.name, Set.empty)
      val dataTypes = usages.map(_.argDef).map(_.kind)
      if (dataTypes.size > 1) {
        val message = s"Param is used with heterogeneous types: " + dataTypes.map(DataTypes.toString).toSeq.sorted.mkString(", ")
        throw newError(message, s"params.${argDef.name}", warnings)
      }
      if (dataTypes.head != argDef.kind) {
        val message = s"Param declared as ${DataTypes.toString(argDef.kind)} is used as ${DataTypes.toString(dataTypes.head)}"
        throw newError(message, s"params.${argDef.name}", warnings)
      }

      // Check the default value is valid w.r.t. every input definition.
      argDef.defaultValue.foreach { defaultValue =>
        val errors = usages.toSeq.flatMap { usage =>
          valueValidator.validate(defaultValue, usage.argDef, Some(s"params.${argDef.name}.default_value"))
        }
        if (errors.nonEmpty) {
          throw new InvalidSpecException(errors)
        }
      }

      // Parameter is optional if either:
      // (1) a default value is defined, or
      // (2) it is used only in optional ports or if a default value is available each time.
      val isOptional = argDef.defaultValue.isDefined || usages.forall(usage => usage.argDef.isOptional || usage.argDef.defaultValue.isDefined)

      argDef.copy(isOptional = isOptional)
    }
  }
}