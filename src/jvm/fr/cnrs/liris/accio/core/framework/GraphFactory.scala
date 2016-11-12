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

package fr.cnrs.liris.accio.core.framework

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.common.util.Seqs

/**
 * Exception thrown if a graph is incorrectly defined (e.g., referencing an unknown operator).
 *
 * @param message Error message.
 * @param cause   Root cause.
 */
class IllegalGraphException(message: String, cause: Throwable = null) extends Exception(message, cause)

/**
 * Factory for [[Graph]].
 *
 * @param opRegistry Operator registry.
 */
@Singleton
final class GraphFactory @Inject()(opRegistry: OpRegistry) {
  /**
   * Create a new graph.
   *
   * @param graphDef Graph definition.
   * @throws IllegalGraphException If the graph definition is invalid.
   */
  @throws[IllegalGraphException]
  def create(graphDef: GraphDef): Graph = {
    // Check for duplicate node names.
    val duplicateNodeNames = graphDef.nodes.groupBy(_.name).filter(_._2.size > 1).keySet
    if (duplicateNodeNames.nonEmpty) {
      throw new IllegalGraphException(s"Duplicate node name: ${duplicateNodeNames.toSeq.sorted.mkString(", ")}")
    }

    // We first create the nodes without specifying any output.
    val freeNodes = graphDef.nodes.map(nodeDef => nodeDef.name -> createNode(nodeDef)).toMap

    // We now connect nodes together. Input dependencies are already defined in [[createNode]], we only have to
    // wire output dependencies correctly.
    val wiredNodes: Set[Node] = freeNodes.values.map { node =>
      node.inputs.foreach { case (thisPort, in) =>
        val thisOp = opRegistry(node.op).defn
        thisOp.inputs.find(_.name == thisPort) match {
          case None => throw new IllegalGraphException(s"Unknown port: ${node.name}/$thisPort")
          case Some(thisArg) =>
            in match {
              case ReferenceInput(ref) =>
                // Check input is defined for a valid operator and port.
                // We could do it sooner, but we are sure here that all op names are valid.
                freeNodes.get(ref.node) match {
                  case None => throw new IllegalGraphException(s"Unknown input predecessor: $ref")
                  case Some(otherNode) =>
                    val otherOp = opRegistry(otherNode.op).defn
                    otherOp.outputs.find(_.name == ref.port) match {
                      case None => throw new IllegalGraphException(s"Unknown input predecessor port: $ref")
                      case Some(otherArg) =>
                        //TODO: allow compatible types (e.g. byte can be converted into int).
                        if (otherArg.kind != thisArg.kind) {
                          throw new IllegalGraphException(s"Data type mismatch: ${otherNode.name}/${ref.port} (${otherArg.kind}) => ${node.name}/$thisPort (${thisArg.kind})")
                        }
                    }
                }
              case _ => // Nothing to check here.
            }
        }
      }

      // Look for all nodes consuming outputs of this one and connect them.
      val outputs = Seqs.index(freeNodes.values.flatMap { otherNode =>
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
    }.toSet

    // We check for cycles now.
    val graph = Graph(wiredNodes)
    if (graph.roots.isEmpty) {
      throw new IllegalGraphException("No root found")
    }
    val cycles = detectCycles(graph)
    if (cycles.nonEmpty) {
      throw new IllegalGraphException(s"Cycles found: ${cycles.map(_.mkString(" -> ")).mkString(", ")}")
    }

    graph
  }

  /**
   * Validate the graph definition would create a valid graph.
   *
   * @param graphDef Graph definition.
   * @return Error message if the graph definition is invalid, empty if everything's fine.
   */
  def validate(graphDef: GraphDef): Option[String] = {
    try {
      create(graphDef)
      None
    } catch {
      case e: IllegalGraphException => Some(e.getMessage)
    }
  }

  private def createNode(nodeDef: NodeDef) = {
    if (Node.NameRegex.findFirstIn(nodeDef.name).isEmpty) {
      throw new IllegalGraphException(s"Invalid node name: ${nodeDef.name} (must match ${Node.NamePattern})")
    }
    opRegistry.get(nodeDef.op) match {
      case None => throw new IllegalGraphException(s"Unknown operator: ${nodeDef.op}")
      case Some(opMeta) =>
        Node(
          name = nodeDef.name,
          op = nodeDef.op,
          inputs = getInputs(nodeDef, opMeta.defn),
          outputs = Map.empty) // Will be populated later.
    }
  }

  private def getInputs(nodeDef: NodeDef, opDef: OpDef): Map[String, Input] = {
    val unknownInputs = nodeDef.inputs.keySet.diff(opDef.inputs.map(_.name).toSet)
    if (unknownInputs.nonEmpty) {
      throw new IllegalGraphException(s"Unknown inputs of ${nodeDef.name}: ${unknownInputs.mkString(", ")}")
    }
    opDef.inputs.map { argDef =>
      val value = nodeDef.inputs.get(argDef.name) match {
        case None => argDef.defaultValue match {
          case Some(defaultValue) => ValueInput(defaultValue)
          case None => throw new IllegalGraphException(s"No value for input: ${nodeDef.name}/${argDef.name}")
        }
        case Some(in: ValueInput) => ValueInput(correctValue(in.value, nodeDef, argDef))
        case Some(in: ReferenceInput) => in
        case Some(in: ParamInput) => ParamInput(in.param, in.defaultValue.map(correctValue(_, nodeDef, argDef)))
      }
      argDef.name -> value
    }.toMap
  }

  private def correctValue(rawValue: Any, nodeDef: NodeDef, argDef: InputArgDef) = {
    val correctedValue = try {
      Values.as(rawValue, argDef.kind)
    } catch {
      case e: IllegalArgumentException =>
        throw new IllegalGraphException(s"Invalid value for ${argDef.kind} input ${nodeDef.name}/${argDef.name}: $rawValue", e)
    }
    if (argDef.isOptional) Some(correctedValue) else correctedValue
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