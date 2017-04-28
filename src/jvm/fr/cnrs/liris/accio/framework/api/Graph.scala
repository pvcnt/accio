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

package fr.cnrs.liris.accio.framework.api

import fr.cnrs.liris.common.util.{Named, Seqs}

import scala.util.matching.Regex

/**
 * An executable graph of operators.
 *
 * @param nodes Nodes forming this graph.
 */
case class Graph private[framework](nodes: Set[Node]) {
  private[this] val index = nodes.map(n => n.name -> n).toMap

  /**
   * Return the size of this graph, i.e., the number of nodes inside.
   */
  def size: Int = nodes.size

  /**
   * Return the set of root nodes.
   */
  def roots: Set[Node] = nodes.filter(_.isRoot)

  /**
   * Return the set of leaf nodes.
   */
  def leaves: Set[Node] = nodes.filter(_.isLeaf)

  /**
   * Return a node by its name, if it exists.
   *
   * @param name Node name.
   */
  def get(name: String): Option[Node] = index.get(name)

  /**
   * Return a node by its name.
   *
   * @param name Node name.
   * @throws NoSuchElementException If thee is no node with given name.
   */
  def apply(name: String): Node = index(name)

  /**
   * Convert this graph into an equivalent Thrift structure.
   */
  def toThrift: thrift.Graph = thrift.Graph(nodes.map(_.toThrift))
}

/**
 * Factory for [[Graph]].
 */
object Graph {
  /**
   * Create a graph from an equivalent Thrift structure. This method assumes the given structure is valid, no
   * validation is done in this method.
   *
   * @param struct Graph, as a Thrift structure.
   */
  def fromThrift(struct: thrift.Graph): Graph = {
    // First create the nodes without specifying any output.
    val nodes = struct.nodes.map(nodeDef => nodeDef.name -> Node.fromThrift(nodeDef)).toMap

    // We now connect nodes together. Input dependencies are already defined when creating the node, we only have to
    // wire output dependencies correctly.
    Graph(nodes.values.map(wireNode(_, nodes)).toSet)
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
        case (otherPort, Input.Reference(ref)) =>
          if (ref.node == node.name) {
            Some(ref.port -> thrift.Reference(otherNode.name, otherPort))
          } else {
            None
          }
        case _ => None
      }
    }.toSet)
    node.copy(outputs = outputs)
  }
}

/**
 * A node inside a graph. A node is a particular instantiation of an operator, with given inputs.
 *
 * @param name    Node name.
 * @param op      Operator name.
 * @param inputs  Inputs of the operator.
 * @param outputs Outputs of the operator, i.e., references to ports of other nodes where outputs are consumed.
 */
case class Node private[framework](
  name: String,
  op: String,
  inputs: Map[String, Input],
  outputs: Map[String, Set[thrift.Reference]]) extends Named {

  /**
   * Check whether this node is a root node, i.e., it does not depend on any other node.
   */
  def isRoot: Boolean = inputs.values.forall {
    case _: Input.Reference => false
    case _ => true
  }

  /**
   * Check whether this node is a leaf node, i.e., no other node depends on it.
   */
  def isLeaf: Boolean = outputs.values.isEmpty

  /**
   * Return the dependencies of this node, i.e., nodes that this node depend on. Dependent nodes must be executed
   * before this node.
   *
   * @return List of node names.
   */
  def dependencies: Set[thrift.Reference] = inputs.values.flatMap {
    case Input.Reference(ref) => Some(ref)
    case _ => None
  }.toSet

  /**
   * Return the predecessors of this node, i.e., nodes that this node depend on.
   *
   * @return List of node names.
   */
  def predecessors: Set[String] = dependencies.map(_.node)

  /**
   * Return the successors of this node, i.e., other nodes that depend on this node.
   *
   * @return List of node names.
   */
  def successors: Set[String] = outputs.values.flatten.map(_.node).toSet

  /**
   * Convert this node into an equivalent Thrift structure.
   */
  private[api] def toThrift: thrift.Node = {
    val inputs = this.inputs.map {
      case (inputName, Input.Param(param)) => inputName -> thrift.Input.Param(param)
      case (inputName, Input.Constant(value)) => inputName -> thrift.Input.Value(value)
      case (inputName, Input.Reference(ref)) => inputName -> thrift.Input.Reference(ref)
    }
    thrift.Node(op, name, inputs)
  }
}

/**
 * Factory for [[Node]].
 */
object Node {
  /**
   * Pattern for valid node names.
   */
  val NamePattern = "[A-Z][a-zA-Z0-9_]+"

  /**
   * Regex for valid node names.
   */
  val NameRegex: Regex = ("^" + NamePattern + "$").r

  /**
   * Create a node from en equivalent Thrift structure.
   *
   * @param struct Node, as a Thrift structure.
   */
  private[api] def fromThrift(struct: thrift.Node): Node = {
    val inputs = struct.inputs.map {
      case (name, thrift.Input.Value(v)) => name -> Input.Constant(v)
      case (name, thrift.Input.Reference(ref)) => name -> Input.Reference(ref)
      case (name, thrift.Input.Param(param)) => name -> Input.Param(param)
      case (name, thrift.Input.UnknownUnionField(_)) =>
        throw new IllegalArgumentException(s"Illegal input ${struct.name}/$name")
    }.toMap
    Node(struct.name, struct.op, inputs, Map.empty)
  }
}

/**
 * An input for a port of a node. It specifies where the value for a given port comes from.
 */
sealed trait Input

/**
 * Values for [[Input]] enumeration.
 */
object Input {

  /**
   * Input defined by a constant value.
   *
   * @param value Input value.
   */
  case class Constant(value: thrift.Value) extends Input

  /**
   * Input coming from the output of another node.
   *
   * @param reference Reference to an output port.
   */
  case class Reference(reference: thrift.Reference) extends Input

  /**
   * Input coming from a workflow parameter. Several ports can use the same parameter name, although they should be
   * of the same data type.
   *
   * @param param Workflow parameter name.
   */
  case class Param(param: String) extends Input
}