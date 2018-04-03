/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

package fr.cnrs.liris.accio.service

import com.google.inject.Inject
import fr.cnrs.liris.accio.api._
import fr.cnrs.liris.accio.api.thrift.FieldViolation

/**
 * Workflow validator.
 *
 * @param graphValidator Graph validator.
 * @param opRegistry     Operator registry.
 */
final class WorkflowValidator @Inject()(graphValidator: GraphValidator, opRegistry: OpRegistry) {
  /**
   * Validate the structure of a workflow.
   *
   * @param workflow Workflow to validate.
   */
  def validate(workflow: Workflow): ValidationResult = {
    val builder = new ValidationResult.Builder
    validate(workflow, builder)
    builder.build
  }

  /**
   * Validate the structure of a workflow.
   *
   * @param workflow Workflow to validate.
   * @param builder  Validation result builder.
   */
  def validate(workflow: Workflow, builder: ValidationResult.Builder): Unit = {
    graphValidator.validate(workflow.graph, builder)
    if (Utils.WorkflowRegex.findFirstIn(workflow.name).isEmpty) {
      builder.error(FieldViolation(s"Invalid workflow name: ${workflow.name} (should match ${Utils.WorkflowPattern})", "name"))
    }
    validateParams(workflow.params, workflow.graph, builder)
  }

  private def validateParams(params: Seq[thrift.ArgDef], graph: Graph, builder: ValidationResult.Builder): Unit = {
    params.zipWithIndex.foreach { case (argDef, idx) =>
      // Check the parameter name is valid.
      if (Utils.ArgRegex.findFirstIn(argDef.name).isEmpty) {
        builder.error(FieldViolation(
          s"Invalid param name: ${argDef.name} (should match ${Utils.ArgPattern})",
          s"params.$idx.name"))
      }

      // Check the default value is consistent with the param type.
      argDef.defaultValue.foreach { value =>
        if (Values.as(value, argDef.kind).isEmpty) {
          builder.error(FieldViolation(
            s"Data type mismatch: requires ${DataTypes.stringify(argDef.kind)}, got ${DataTypes.stringify(value.kind)}",
            s"params.$idx.defaultValue"))
        }
      }
    }

    // Check that params usage is consistent with their definition.
    graph.nodes.zipWithIndex.foreach { case (node, idx) =>
      node.inputs.foreach {
        case (name, Input.Param(paramName)) =>
          params.find(_.name == paramName) match {
            case None => builder.error(FieldViolation(s"Unknown param: $paramName", s"graph.$idx.inputs.$name"))
            case Some(paramDef) =>
              opRegistry.get(node.op).flatMap(_.inputs.find(_.name == name)).foreach { argDef =>
                if (argDef.kind != paramDef.kind) {
                  builder.error(FieldViolation(
                    s"Data type mismatch: requires ${DataTypes.stringify(argDef.kind)}, got ${DataTypes.stringify(paramDef.kind)}",
                    s"graph.$idx.inputs.$name"))
                }
              }
          }
        case _ => // Nothing to check here.
      }
    }
  }
}