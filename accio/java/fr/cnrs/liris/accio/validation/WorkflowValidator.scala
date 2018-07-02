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

package fr.cnrs.liris.accio.validation

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.discovery.OpRegistry
import fr.cnrs.liris.accio.domain._
import fr.cnrs.liris.lumos.domain.{AttrValue, DataType, Value}

import scala.util.Random
import scala.util.matching.Regex

/**
 * Workflow validator, in charge of ensuring the workflow objects have a valid structure.
 *
 * @param registry      Operator registry.
 * @param nameGenerator Name generator.
 */
@Singleton
final class WorkflowValidator @Inject()(registry: OpRegistry, nameGenerator: NameGenerator) {

  import WorkflowValidator._

  /**
   * Prepare the definition of a workflow before it is validated. This does not validate the
   * workflow nor throw any exception, but normalizes as much as possible the workflow (e.g., by
   * filling fields that may have been omitted).
   *
   * @param workflow Workflow definition.
   * @param user Current user's name.
   */
  def prepare(workflow: Workflow, user: Option[String]): Workflow = {
    val name = if (workflow.name.isEmpty) nameGenerator.generateName() else workflow.name
    val params = workflow.params.map(prepareParam(_, workflow))
    val seed = if (workflow.seed == 0) Random.nextLong() else workflow.seed
    val steps = prepareSteps(workflow)
    workflow.copy(
      name = name,
      owner = user.orElse(workflow.owner),
      params = params,
      seed = seed,
      steps = steps)
  }

  private def prepareParam(param: AttrValue, workflow: Workflow): AttrValue = {
    // We collect the data types as which the parameter is actually used. If only one is used, we
    // then attempt to cast the parameter into that particular type.
    val dataTypes = workflow.steps.flatMap { step =>
      step.params.flatMap {
        case Channel(name, Channel.Param(paramName)) if paramName == param.name =>
          registry.get(step.op).toSeq.flatMap { op =>
            op.inputs.find(_.name == name).map(_.dataType).toSet
          }
        case _ => Set.empty
      }
    }.toSet
    if (dataTypes.size == 1) {
      param.value.cast(dataTypes.head)
        .map(v => param.copy(value = v))
        .getOrElse(param)
    } else {
      param
    }
  }

  private def prepareSteps(workflow: Workflow): Seq[Step] = {
    workflow.steps.map { step =>
      val op = registry.get(step.op)
      val params = step.params.zipWithIndex.map { case (channel, idx) =>
        // If a channel name is omitted, we try to find a matching positional input on the
        // associated operator, and fill the name accordingly. Because the order of inputs might
        // change later on, this allows to explicitly keep track of input values without ambiguity.
        val name = if (channel.name.isEmpty && op.isDefined && idx < op.get.inputs.length) {
          op.get.inputs(idx).name
        } else {
          channel.name
        }
        channel.copy(name = name)
      }
      // If step name is omitted, it defaults to the operator's name. It means that if an operator
      // appears multiple times in a workflow, associated steps must have their names redefined.
      val name = if (step.name.isEmpty) step.op else step.name
      step.copy(name = name, params = params)
    }
  }

  /**
   * Validate the definition of a workflow. This method does not throw any exception in case the
   * workflow is invalid, but return every error found as a [[ValidationResult]]. You should
   * usually call [[WorkflowValidator.prepare]] before calling this method.
   *
   * @param workflow Workflow definition.
   */
  def validate(workflow: Workflow): ValidationResult = {
    val builder = new ValidationResult.Builder
    validateName(workflow.name, "name", builder)
    validateRepeat(workflow.repeat, builder)
    workflow.params.zipWithIndex.foreach { case (attr, idx) => validateParam(attr, idx, workflow, builder) }
    validateGraph(Graph.create(workflow), workflow, builder)
    builder.build
  }

  private def validateName(name: String, field: String, builder: ValidationResult.Builder): Unit = {
    if (NameRegex.findFirstIn(name).isEmpty) {
      builder.error(ValidationResult.FieldViolation(s"Illegal value: $name (should match $NamePattern)", field))
    }
  }

  private def validateRepeat(repeat: Int, builder: ValidationResult.Builder): Unit = {
    if (repeat < 1) {
      builder.error(ValidationResult.FieldViolation(s"Illegal value: $repeat (should match be at least 1)", "repeat"))
    }
  }

  private def validateParam(param: AttrValue, idx: Int, job: Workflow, builder: ValidationResult.Builder): Unit = {
    validateName(param.name, s"params.$idx.name", builder)

    val count = job.params.count(_.name == param.name)
    if (count > 1) {
      builder.error(ValidationResult.FieldViolation(
        s"Duplicate parameter name: ${param.name} (appears $count times)",
        s"params.$idx.name"))
    }
  }

  private def validateGraph(graph: Graph, workflow: Workflow, builder: ValidationResult.Builder): Unit = {
    // Validate each step individually.
    workflow.steps.zipWithIndex.foreach { case (step, idx) => validateStep(step, idx, graph, workflow, builder) }

    // Validate the graph is a directed acyclic graph (DAG).
    if (graph.roots.isEmpty) {
      builder.error(ValidationResult.FieldViolation("No root step", "steps"))
    } else {
      val cycles = detectCycles(graph)
      if (cycles.nonEmpty) {
        cycles.foreach { cycle =>
          builder.error(ValidationResult.FieldViolation(s"Cycle detected: ${cycle.mkString(" -> ")}", "steps"))
        }
      }
    }
  }

  private def validateStep(step: Step, idx: Int, graph: Graph, job: Workflow, builder: ValidationResult.Builder): Unit = {
    // Validate step name.
    validateName(step.name, s"steps.$idx.name", builder)

    val count = job.steps.count(_.name == step.name)
    if (count > 1) {
      builder.error(ValidationResult.FieldViolation(
        s"Duplicate step name: ${step.name} (appears $count times)",
        s"steps.$idx.name"))
    }

    // Validate that the referenced operator exists, and that the step matches its definition.
    registry.get(step.op) match {
      case None => builder.error(ValidationResult.FieldViolation(s"Unknown operator: ${step.op}", s"steps.$idx.op"))
      case Some(op) => validateStep(step, op, idx, job, builder)
    }
  }

  private def validateStep(step: Step, op: Operator, idx: Int, workflow: Workflow, builder: ValidationResult.Builder): Unit = {
    op.deprecation.foreach { message =>
      builder.warn(ValidationResult.FieldViolation(s"Operator ${step.op} is deprecated: $message", s"steps.$idx.op"))
    }

    op.inputs.foreach { attr =>
      if (!attr.optional && attr.defaultValue.isEmpty && !step.params.exists(_.name == attr.name)) {
        builder.error(ValidationResult.FieldViolation(
          s"Required parameter for operator ${step.op} is missing: ${attr.name}",
          s"steps.$idx.inputs"))
      }
    }

    step.params.foreach { case Channel(name, source) =>
      op.inputs.find(_.name == name) match {
        case None =>
          builder.warn(ValidationResult.FieldViolation(
            s"Unknown parameter for operator ${op.name}: $name",
            s"steps.$idx.inputs.$name"))
        case Some(attr) =>
          source match {
            case Channel.Reference(stepName, outputName) =>
              workflow.steps.find(_.name == stepName) match {
                case None =>
                  builder.error(ValidationResult.FieldViolation(
                    s"Unknown step: $stepName",
                    s"steps.$idx.inputs.$name.step"))
                case Some(otherStep) =>
                  registry.get(otherStep.op).foreach { otherOp =>
                    otherOp.outputs.find(_.name == outputName) match {
                      case None =>
                        builder.error(ValidationResult.FieldViolation(
                          s"Unknown output for operator ${otherOp.name}: $stepName/$outputName",
                          s"steps.$idx.inputs.$name.port"))
                      case Some(otherAttr) =>
                        if (attr.dataType != otherAttr.dataType) {
                          // TODO: support casting from one supported type to another.
                          builder.error(ValidationResult.FieldViolation(
                            s"Data type mismatch: requires ${attr.dataType}, got ${otherAttr.dataType}",
                            s"steps.$idx.inputs.$name"))
                        }
                    }
                  }
              }
            case Channel.Constant(v) =>
              if (!isAssignableFrom(attr.dataType, v)) {
                builder.error(ValidationResult.FieldViolation(
                  s"Data type mismatch: requires ${attr.dataType}, got ${v.dataType}",
                  s"steps.$idx.inputs.$name.constant"))
              }
            case Channel.Param(paramName) =>
              workflow.params.find(_.name == paramName) match {
                case None => builder.error(ValidationResult.FieldViolation(
                  s"Unknown parameter: $paramName",
                  s"steps.$idx.inputs.$name.param"))
                case Some(param) =>
                  if (!isAssignableFrom(attr.dataType, param.value)) {
                    builder.error(ValidationResult.FieldViolation(
                      s"Data type mismatch: requires ${attr.dataType}, got ${param.value.dataType}",
                      s"steps.$idx.inputs.$name.param"))
                  }
              }
          }
      }
    }
  }

  private def isAssignableFrom(dataType: DataType, value: Value): Boolean = {
    dataType == value.dataType || value.cast(dataType).isDefined
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

object WorkflowValidator {
  /**
   * Pattern for valid names.
   */
  private val NamePattern = "[a-zA-Z0-9][a-zA-Z0-9._-]*"

  /**
   * Regex for valid names.
   */
  private val NameRegex: Regex = ("^" + NamePattern + "$").r
}