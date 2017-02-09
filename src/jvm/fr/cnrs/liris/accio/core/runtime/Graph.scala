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

import fr.cnrs.liris.accio.core.domain.Reference
import fr.cnrs.liris.common.util.Named
import fr.cnrs.liris.dal.core.api.Value

/**
 * An executable graph of operators.
 *
 * @param nodes Nodes forming this graph.
 */
case class Graph private[runtime](nodes: Set[Node]) {
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
  @throws[NoSuchElementException]
  def apply(name: String): Node = index(name)
}

/**
 * A node inside a graph. A node is a particular instantiation of an operator, with given inputs.
 *
 * @param name    Node name.
 * @param op      Operator name.
 * @param inputs  Inputs of the operator.
 * @param outputs Outputs of the operator, i.e., references to ports of other nodes where outputs are consumed.
 */
case class Node private[runtime](
  name: String,
  op: String,
  inputs: Map[String, Input],
  outputs: Map[String, Set[Reference]]) extends Named {

  /**
   * Check whether this node is a root node, i.e., it does not depend on any other node.
   */
  def isRoot: Boolean = inputs.values.forall {
    case _: ReferenceInput => false
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
  def dependencies: Set[Reference] = inputs.values.flatMap {
    case ReferenceInput(ref) => Some(ref)
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
}

/**
 * An input for a port of a node. It specifies where the value for a given port comes from.
 */
sealed trait Input

/**
 * Input defined by a constant value.
 *
 * @param value Input value.
 */
case class ValueInput(value: Value) extends Input

/**
 * Input coming from the output of another node.
 *
 * @param reference Reference to an output port.
 */
case class ReferenceInput(reference: Reference) extends Input

/**
 * Input coming from a workflow parameter. Several ports can use the same parameter name, although they should be
 * of the same data type.
 *
 * @param param Workflow parameter name.
 */
case class ParamInput(param: String) extends Input