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

import com.google.inject.Inject
import fr.cnrs.liris.accio.framework.api._
import fr.cnrs.liris.accio.framework.api.thrift
import fr.cnrs.liris.accio.framework.api.thrift.{InvalidSpecException, InvalidSpecMessage}
import fr.cnrs.liris.dal.core.api.DataTypes

import scala.collection.mutable

/**
 * Factory and validation for converting a [[Graph]] from a Thrift structure to a Scala object. This relies on
 * [[Graph.fromThrift]] to create the Scala object, but performs validity and consistency checks.
 *
 * @param opRegistry Operator registry.
 */
final class GraphFactory @Inject()(opRegistry: OpRegistry) extends BaseFactory {
  /**
   * Convert a graph from a Thrift structure to a valid Scala object.
   *
   * @param struct   Graph, as a Thrift structure.
   * @param warnings Mutable list collecting warnings.
   * @throws InvalidSpecException If the graph definition is invalid.
   */
  @throws[InvalidSpecException]
  def create(struct: thrift.Graph, warnings: mutable.Set[InvalidSpecMessage] = mutable.Set.empty[InvalidSpecMessage]): Graph = {
    // Check for duplicate node names. This is our first check, as it will be fatal for the indexing of nodes done
    // inside the `Graph`.
    val duplicateNodeNames = struct.nodes.groupBy(_.name).filter(_._2.size > 1).keySet
    if (duplicateNodeNames.nonEmpty) {
      throw newError("Duplicate node name", duplicateNodeNames.map(name => s"graph.$name"), warnings)
    }

    // Now we create the graph. It is possible to create it as long as their is no duplicate node name. Next we
    // validate it is correctly defined.
    val graph = Graph.fromThrift(struct)
    validateGraph(graph, warnings)
    graph
  }

  /**
   * Validate a node is consistent with its definition.
   *
   * @param node     Node.
   * @param graph    Graph this node belongs to.
   * @param warnings Mutable list collecting warnings.
   */
  private def validateNode(node: Node, graph: Graph, warnings: mutable.Set[InvalidSpecMessage]) = {
    // Validate node name.
    if (Node.NameRegex.findFirstIn(node.name).isEmpty) {
      throw newError(s"Invalid node name: ${node.name} (should match ${Node.NamePattern})", warnings)
    }

    // Validate node corresponds to an actual operator, and then its inputs.
    opRegistry.get(node.op) match {
      case None => throw newError(s"Unknown operator: ${node.op}", s"graph.${node.name}.op", warnings)
      case Some(opDef) => validateInputs(node, opDef, warnings)
    }

    // Validate references to other nodes.
    validateReferences(node, graph, warnings)
  }

  /**
   * Validate the inputs of a node are consistent with its definition.
   *
   * @param node     Node.
   * @param opDef    Operator definition (matching node's op).
   * @param warnings Mutable list collecting warnings.
   */
  private def validateInputs(node: Node, opDef: thrift.OpDef, warnings: mutable.Set[InvalidSpecMessage]) = {
    val unknownInputs = node.inputs.keySet.diff(opDef.inputs.map(_.name).toSet)
    if (unknownInputs.nonEmpty) {
      throw newError("Unknown input port", unknownInputs.map(name => s"graph.${node.name}.inputs.$name"), warnings)
    }
    opDef.inputs.foreach { argDef =>
      node.inputs.get(argDef.name) match {
        case None =>
          val hasDefaultValue = argDef.defaultValue.map(Input.Constant.apply).nonEmpty
          if (!hasDefaultValue && !argDef.isOptional) {
            throw newError(s"No value for required input", s"graph.${node.name}.inputs.${argDef.name}", warnings)
          }
        case Some(Input.Constant(v)) =>
          if (v.kind != argDef.kind) {
            throw newError(
              s"Data type mismatch: requires ${DataTypes.toString(argDef.kind)}, got ${DataTypes.toString(v.kind)}",
              s"graph.${node.name}.inputs.${argDef.name}",
              warnings)
          }
        case _ =>
      }
    }
  }

  /**
   * Validate the input references of a node are consistent with other nodes inside the graph.
   *
   * @param node     Node to validate.
   * @param graph    Graph this node belongs to.
   * @param warnings Mutable list collecting warnings.
   */
  private def validateReferences(node: Node, graph: Graph, warnings: mutable.Set[InvalidSpecMessage]): Unit = {
    // Operator existence has already been validated previously in `validateNode`.
    val thisOp = opRegistry(node.op)
    thisOp.deprecation.foreach { message =>
      warnings += InvalidSpecMessage(s"Operator is deprecated: $message", Some(s"graph.${node.name}"))
    }

    node.inputs.foreach {
      case (thisPort, Input.Reference(ref)) =>
        // Check input is defined for a valid operator and port, and data types are consistent.
        // We could do it sooner, but we are sure here that all op names are valid.
        graph.get(ref.node) match {
          case None => throw newError(s"Unknown node: ${ref.node}", s"graph.${node.name}.inputs.$thisPort", warnings)
          case Some(otherNode) =>
            val otherOp = opRegistry(otherNode.op)
            otherOp.outputs.find(_.name == ref.port) match {
              case None =>
                throw newError(
                  s"Unknown output port: ${ref.node}/${ref.port}",
                  s"graph.${node.name}.inputs.$thisPort",
                  warnings)
              case Some(otherArg) =>
                val thisArg = thisOp.inputs.find(_.name == thisPort).get
                if (otherArg.kind != thisArg.kind) {
                  throw newError(
                    s"Data type mismatch: requires ${DataTypes.toString(thisArg.kind)}, got ${DataTypes.toString(otherArg.kind)}",
                    s"graph.${node.name}.inputs.$thisPort",
                    warnings)
                }
            }
        }
      case _ => // Nothing to check here.
    }
  }

  /**
   * Validate the entire graph.
   *
   * @param graph    Graph to validate.
   * @param warnings Mutable list collecting warnings.
   */
  private def validateGraph(graph: Graph, warnings: mutable.Set[InvalidSpecMessage]): Unit = {
    // Validate each node individually.
    graph.nodes.foreach(validateNode(_, graph, warnings))

    // Validate the graph is a directed acyclic graph (DAG).
    if (graph.roots.isEmpty) {
      throw newError("No root node", warnings)
    }
    val cycles = detectCycles(graph)
    if (cycles.nonEmpty) {
      val messages = cycles.map(cycle => s"Cycle detected: ${cycle.mkString(" -> ")}")
      throw newError(messages, warnings)
    }
  }

  /**
   * Detect cycles inside a graph.
   *
   * @param graph Graph to validate.
   * @return List of cycles, each cycle being the sequence of node names composing it.
   */
  private def detectCycles(graph: Graph): Set[Seq[String]] = {
    graph.roots.map(node => visit(graph, node.name, Seq.empty)).filter(_.nonEmpty)
  }

  /**
   * Recursively visit nodes of a graph.
   *
   * @param graph    Graph to visit.
   * @param nodeName Current name of node to visit.
   * @param visited  Previously visited node names.
   * @return New list of visited node names.
   */
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