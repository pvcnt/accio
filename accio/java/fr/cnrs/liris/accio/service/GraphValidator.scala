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
import fr.cnrs.liris.accio.api.thrift.FieldViolation
import fr.cnrs.liris.accio.api.{thrift, _}

/**
 * Graph validator.
 *
 * @param opRegistry Operator registry.
 */
final class GraphValidator @Inject()(opRegistry: OpRegistry) {
  /**
   * Validate the definition of a graph.
   *
   * @param graph Graph to validate.
   */
  def validate(graph: Graph): ValidationResult = {
    val builder = new ValidationResult.Builder
    validate(graph, builder)
    builder.build
  }

  /**
   * Validate the definition of a graph.
   *
   * @param graph   Graph to validate.
   * @param builder Validation result builder.
   */
  def validate(graph: Graph, builder: ValidationResult.Builder): Unit = {
    // Check for duplicate node names.
    val duplicateNames = graph.nodes.groupBy(_.name).filter(_._2.size > 1).keySet
    duplicateNames.foreach(name => builder.error(FieldViolation(s"Duplicate node name: $name", "graph")))

    // Validate each node individually.
    graph.nodes.zipWithIndex.foreach { case (node, idx) => validateNode(node, idx, graph, builder) }

    // Validate the graph is a directed acyclic graph (DAG).
    if (graph.roots.isEmpty) {
      builder.error(FieldViolation("No root node", "graph"))
    } else {
      val cycles = detectCycles(graph)
      if (cycles.nonEmpty) {
        cycles.foreach(cycle => builder.error(FieldViolation(s"Cycle detected: ${cycle.mkString(" -> ")}", "graph")))
      }
    }

    builder.build
  }

  private def validateNode(node: Node, idx: Int, graph: Graph, builder: ValidationResult.Builder): Unit = {
    // Validate node name.
    if (Node.NameRegex.findFirstIn(node.name).isEmpty) {
      builder.error(FieldViolation(s"Invalid node name: ${node.name} (should match ${Node.NamePattern})", s"graph.$idx"))
    }

    // Validate that each node corresponds to an actual operator, and then its definition.
    opRegistry.get(node.op) match {
      case None => builder.error(FieldViolation(s"Unknown operator: ${node.op}", s"graph.$idx.op"))
      case Some(opDef) => validateNode(node, idx, graph, opDef, builder)
    }
  }

  private def validateNode(node: Node, idx: Int, graph: Graph, opDef: thrift.OpDef, builder: ValidationResult.Builder): Unit = {
    opDef.deprecation.foreach { message =>
      builder.warn(FieldViolation(s"Operator is deprecated: $message", s"graph.$idx.op"))
    }

    opDef.inputs.foreach { argDef =>
      if (argDef.defaultValue.isEmpty && !node.inputs.contains(argDef.name)) {
        builder.error(FieldViolation(
          s"Required input is missing: ${argDef.name}",
          s"graph.$idx.inputs"))
      }
    }

    node.inputs.foreach { case (name, input) =>
      opDef.inputs.find(_.name == name) match {
        case None => builder.error(FieldViolation(
          s"Unknown input for operator ${opDef.name}",
          s"graph.$idx.inputs.$name"))
        case Some(argDef) =>
          input match {
            case Input.Reference(ref) =>
              // Check input is defined for a valid operator and port, and data types are consistent.
              // We could do it sooner, but we are sure here that all op names are valid.
              graph.get(ref.node) match {
                case None =>
                  builder.error(FieldViolation(
                    s"Reference to unknown node: ${ref.node}",
                    s"graph.$idx.inputs.$name"))
                case Some(otherNode) =>
                  opRegistry.get(otherNode.op).foreach { otherOp =>
                    otherOp.outputs.find(_.name == ref.port) match {
                      case None =>
                        builder.error(FieldViolation(
                          s"Unknown output port for operator ${otherOp.name}: ${ref.node}/${ref.port}",
                          s"graph.$idx.inputs.$name"))
                      case Some(otherArg) =>
                        if (argDef.kind != otherArg.kind) {
                          builder.error(FieldViolation(
                            s"Data type mismatch: requires ${DataTypes.stringify(argDef.kind)}, got ${DataTypes.stringify(otherArg.kind)}",
                            s"graph.$idx.inputs.$name"))
                        }
                    }
                  }
              }
            case Input.Constant(v) =>
              if (Values.as(v, argDef.kind).isEmpty) {
                builder.error(FieldViolation(
                  s"Data type mismatch: requires ${DataTypes.stringify(argDef.kind)}, got ${DataTypes.stringify(v.kind)}",
                  s"graph.$idx.inputs.${argDef.name}"))
              }
            case Input.Param(_) => // Nothing to check here.
          }
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