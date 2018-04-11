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

package fr.cnrs.liris.accio.api

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.api.thrift.{Step => _, _}

import scala.util.matching.Regex

/**
 * Job validator.
 *
 * @param opRegistry Operator registry.
 */
@Singleton
final class JobValidator @Inject()(opRegistry: OpRegistry) {

  import JobValidator._

  /**
   * Validate the definition of a job.
   *
   * @param job Job to validate.
   */
  def validate(job: Job): ValidationResult = {
    val builder = new ValidationResult.Builder
    validateName(job.name, "name", builder)
    job.params.zipWithIndex.foreach { case (attr, idx) => validateParam(attr, idx, job, builder) }
    validateGraph(Graph.fromThrift(job.steps), job, builder)
    builder.build
  }

  private def validateName(name: String, field: String, builder: ValidationResult.Builder): Unit = {
    if (NameRegex.findFirstIn(name).isEmpty) {
      builder.error(FieldViolation(s"Illegal name: $name (should match $NamePattern)", field))
    }
  }

  private def validateParam(param: NamedValue, idx: Int, job: Job, builder: ValidationResult.Builder): Unit = {
    validateName(param.name, s"params.$idx.name", builder)

    val count = job.params.count(_.name == param.name)
    if (count > 1) {
      builder.error(FieldViolation(
        s"Duplicate parameter name: ${param.name} (appears $count times)",
        s"params.$idx.name"))
    }
  }

  private def validateGraph(graph: Graph, job: Job, builder: ValidationResult.Builder): Unit = {
    // Validate each step individually.
    graph.steps.zipWithIndex.foreach { case (step, idx) => validateStep(step, idx, graph, job, builder) }

    // Validate the graph is a directed acyclic graph (DAG).
    if (graph.roots.isEmpty) {
      builder.error(FieldViolation("No root step", "steps"))
    } else {
      val cycles = detectCycles(graph)
      if (cycles.nonEmpty) {
        cycles.foreach(cycle => builder.error(FieldViolation(s"Cycle detected: ${cycle.mkString(" -> ")}", "steps")))
      }
    }
  }

  private def validateStep(step: Step, idx: Int, graph: Graph, job: Job, builder: ValidationResult.Builder): Unit = {
    // Validate step name.
    validateName(step.name, s"steps.$idx.name", builder)

    val count = graph.steps.count(_.name == step.name)
    if (count > 1) {
      builder.error(FieldViolation(
        s"Duplicate step name: ${step.name} (appears $count times)",
        s"steps.$idx.name"))
    }

    // Validate that the referenced operator exists, and that the step matches its definition.
    opRegistry.get(step.op) match {
      case None => builder.error(FieldViolation(s"Unknown operator: ${step.op}", s"steps.$idx.op"))
      case Some(op) => validateStep(step, op, idx, graph, job, builder)
    }
  }

  private def validateStep(step: Step, op: Operator, idx: Int, graph: Graph, experiment: Job, builder: ValidationResult.Builder): Unit = {
    op.deprecation.foreach { message =>
      builder.warn(FieldViolation(s"Operator ${step.op} is deprecated: $message", s"steps.$idx.op"))
    }

    op.inputs.foreach { attr =>
      if (!attr.isOptional && attr.defaultValue.isEmpty && !step.inputs.exists(_.name == attr.name)) {
        builder.error(FieldViolation(
          s"Required parameter for operator ${step.op} is missing: ${attr.name}",
          s"steps.$idx.inputs"))
      }
    }

    step.inputs.foreach { case NamedChannel(name, channel) =>
      op.inputs.find(_.name == name) match {
        case None =>
          builder.warn(FieldViolation(
            s"Unknown parameter for operator ${op.name}: $name",
            s"steps.$idx.inputs.$name"))
        case Some(attr) =>
          channel match {
            case Channel.Reference(ref) =>
              graph.get(ref.step) match {
                case None =>
                  builder.error(FieldViolation(
                    s"Unknown step: ${ref.step}",
                    s"steps.$idx.inputs.$name.step"))
                case Some(otherStep) =>
                  opRegistry.get(otherStep.op).foreach { otherOp =>
                    otherOp.outputs.find(_.name == ref.output) match {
                      case None =>
                        builder.error(FieldViolation(
                          s"Unknown output for operator ${otherOp.name}: ${ref.step}/${ref.output}",
                          s"steps.$idx.inputs.$name.port"))
                      case Some(otherAttr) =>
                        if (attr.dataType != otherAttr.dataType) {
                          builder.error(FieldViolation(
                            s"Data type mismatch: requires ${DataTypes.stringify(attr.dataType)}, got ${DataTypes.stringify(otherAttr.dataType)}",
                            s"steps.$idx.inputs.$name"))
                        }
                    }
                  }
              }
            case Channel.Value(v) =>
              if (v.dataType != attr.dataType) {
                builder.error(FieldViolation(
                  s"Data type mismatch: requires ${DataTypes.stringify(attr.dataType)}, got ${DataTypes.stringify(v.dataType)}",
                  s"steps.$idx.inputs.$name.value"))
              }
            case Channel.Param(paramName) =>
              experiment.params.find(_.name == paramName) match {
                case None => builder.error(FieldViolation(
                  s"Unknown parameter: $paramName",
                  s"steps.$idx.inputs.$name.param"))
                case Some(param) =>
                  if (attr.dataType != param.value.dataType) {
                    builder.error(FieldViolation(
                      s"Data type mismatch: requires ${DataTypes.stringify(attr.dataType)}, got ${DataTypes.stringify(param.value.dataType)}",
                      s"steps.$idx.inputs.$name.param"))
                  }
              }
            case Channel.UnknownUnionField(_) =>
          }
      }
    }

    step.exports.zipWithIndex.foreach { case (export, idx2) =>
      validateName(export.exportAs, s"steps.$idx.exports.$idx2", builder)
      if (!op.outputs.exists(_.name == export.output)) {
        builder.warn(FieldViolation(
          s"Unknown artifact for operator ${op.name}: ${export.output}",
          s"steps.$idx.inputs.$idx2"))
      }
    }
  }

  private def detectCycles(graph: Graph): Set[Seq[String]] = {
    graph.roots.map(node => visit(graph, node.name, Seq.empty)).filter(_.nonEmpty)
  }

  private def visit(graph: Graph, nodeName: String, visited: Seq[String]): Seq[String] = {
    val node = graph(nodeName)
    if (visited.contains(nodeName)) {
      visited.drop(visited.indexOf(nodeName)) ++ Seq(nodeName)
    } else if (node.successors.isEmpty) {
      Seq.empty
    } else {
      node.successors.toSeq.flatMap(visit(graph, _, visited ++ Seq(nodeName)))
    }
  }
}

object JobValidator {
  /**
   * Pattern for valid names.
   */
  private val NamePattern = "[a-zA-Z][a-zA-Z0-9-]+"

  /**
   * Regex for valid names.
   */
  private val NameRegex: Regex = ("^" + NamePattern + "$").r
}