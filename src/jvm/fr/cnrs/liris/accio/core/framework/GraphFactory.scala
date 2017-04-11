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

package fr.cnrs.liris.accio.core.framework

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.api.Utils
import fr.cnrs.liris.accio.core.api.thrift._
import fr.cnrs.liris.common.util.Seqs
import fr.cnrs.liris.dal.core.api.DataTypes

import scala.collection.mutable

/**
 * Factory for [[Graph]]. Graph definitions are used to define graph in a compact format, but may need to be converted
 * into [[Graph]]s to be more exploitable.
 *
 * @param opRegistry Operator registry.
 */
final class GraphFactory @Inject()(opRegistry: OpRegistry) extends BaseFactory {
  /**
   * Create a new graph from a graph definition.
   *
   * @param graphDef Graph definition.
   * @param warnings Mutable list collecting warnings.
   * @throws InvalidSpecException If the graph definition is invalid.
   */
  @throws[InvalidSpecException]
  def create(graphDef: GraphDef, warnings: mutable.Set[InvalidSpecMessage] = mutable.Set.empty[InvalidSpecMessage]): Graph = {
    // Check for duplicate node names.
    val duplicateNodeNames = graphDef.nodes.groupBy(_.name).filter(_._2.size > 1).keySet
    if (duplicateNodeNames.nonEmpty) {
      throw newError("Duplicate node name", duplicateNodeNames.map(name => s"graph.$name"), warnings)
    }

    // First create the nodes without specifying any output.
    val nodes = graphDef.nodes.map(nodeDef => nodeDef.name -> createNode(nodeDef, warnings)).toMap

    // We now connect nodes together. Input dependencies are already defined when creating the node, we only have to
    // wire output dependencies correctly.
    val wiredNodes: Set[Node] = nodes.values.map { node =>
      validateNode(node, nodes, warnings)
      wireNode(node, nodes)
    }.toSet

    val graph = Graph(wiredNodes)
    validateGraph(graph, warnings)
    graph
  }

  /**
   * Create a node from its definition. Check the operator does exist.
   *
   * @param nodeDef  Node definition.
   * @param warnings Mutable list collecting warnings.
   */
  private def createNode(nodeDef: NodeDef, warnings: mutable.Set[InvalidSpecMessage]) = {
    opRegistry.get(nodeDef.op) match {
      case None => throw newError(s"Unknown operator: ${nodeDef.op}", s"graph.${nodeDef.name}.op", warnings)
      case Some(opDef) =>
        // Outputs will be populated later.
        Node(nodeDef.name, nodeDef.op, createInputs(nodeDef, opDef, warnings), Map.empty)
    }
  }

  /**
   * Create the inputs of a node, from its definition. Check the input is defined (if non-optional) and valid.
   *
   * @param nodeDef  Node definition.
   * @param opDef    Operator definition (matching node's op).
   * @param warnings Mutable list collecting warnings.
   */
  private def createInputs(nodeDef: NodeDef, opDef: OpDef, warnings: mutable.Set[InvalidSpecMessage]): Map[String, Input] = {
    val unknownInputs = nodeDef.inputs.keySet.diff(opDef.inputs.map(_.name).toSet)
    if (unknownInputs.nonEmpty) {
      throw newError("Unknown input port", unknownInputs.map(name => s"graph.${nodeDef.name}.inputs.$name"), warnings)
    }
    opDef.inputs.flatMap { argDef =>
      val value = nodeDef.inputs.get(argDef.name) match {
        case None =>
          val defaultValue = argDef.defaultValue.map(ValueInput.apply)
          if (defaultValue.isEmpty && !argDef.isOptional) {
            throw newError(s"No value for required input", s"graph.${nodeDef.name}.inputs.${argDef.name}", warnings)
          }
          defaultValue
        case Some(InputDef.Value(v)) =>
          if (v.kind != argDef.kind) {
            throw newError(
              s"Data type mismatch: requires ${DataTypes.toString(argDef.kind)}, got ${DataTypes.toString(v.kind)}",
              s"graph.${nodeDef.name}.inputs.${argDef.name}",
              warnings)
          }
          Some(ValueInput(v))
        case Some(InputDef.Reference(ref)) => Some(ReferenceInput(ref))
        case Some(InputDef.Param(name)) => Some(ParamInput(name))
        case Some(InputDef.UnknownUnionField(_)) =>
          throw newError("Illegal input", s"graph.${nodeDef.name}.inputs.${argDef.name}", warnings)
      }
      value.map(v => argDef.name -> v)
    }.toMap
  }

  /**
   * Validate a single node.
   *
   * @param node     Node to validate.
   * @param nodes    All nodes inside the graph, indexed by name. They have not necessarily been validated.
   * @param warnings Mutable list collecting warnings.
   */
  private def validateNode(node: Node, nodes: Map[String, Node], warnings: mutable.Set[InvalidSpecMessage]): Unit = {
    if (Utils.NodeRegex.findFirstIn(node.name).isEmpty) {
      throw newError(s"Invalid node name: ${node.name} (should match ${Utils.NodePattern})", warnings)
    }

    // Operator existence has already been validated previously in `createNode`.
    val thisOp = opRegistry(node.op)
    thisOp.deprecation.foreach { message =>
      warnings += InvalidSpecMessage(s"Operator is deprecated: $message", Some(s"graph.${node.name}"))
    }

    node.inputs.foreach {
      case (thisPort, ReferenceInput(ref)) =>
        // Check input is defined for a valid operator and port, and data types are consistent.
        // We could do it sooner, but we are sure here that all op names are valid.
        nodes.get(ref.node) match {
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
   * Create connections between outputs of a node and all other nodes consuming them.
   *
   * @param node  Node to connect.
   * @param nodes All nodes inside the graph, indexed by name. They have already been validated.
   * @return Updated node, with outputs connected.
   */
  private def wireNode(node: Node, nodes: Map[String, Node]): Node = {
    val outputs = Seqs.index(nodes.values.flatMap { otherNode =>
      otherNode.inputs.flatMap {
        case (otherPort, ReferenceInput(ref)) =>
          if (ref.node == node.name) {
            Some(ref.port -> Reference(otherNode.name, otherPort))
          } else {
            None
          }
        case _ => None
      }
    }.toSet)
    node.copy(outputs = outputs)
  }

  /**
   * Validate that a graph is actually a directed acyclic graph (DAG).
   *
   * @param graph    Graph to validate.
   * @param warnings Mutable list collecting warnings.
   */
  private def validateGraph(graph: Graph, warnings: mutable.Set[InvalidSpecMessage]): Unit = {
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