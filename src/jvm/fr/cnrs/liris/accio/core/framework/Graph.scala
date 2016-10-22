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

import com.fasterxml.jackson.annotation._
import com.twitter.finatra.domain.WrappedValue
import fr.cnrs.liris.common.util.Named

import scala.annotation.meta.getter

/**
 * An executable graph of operators.
 *
 * No validation is performed inside the constructor, we assume the graph is valid. You can use a [[GraphFactory]]
 * for this purpose. A graph is usually embedded as part of a [[Workflow]].
 *
 * @param nodes Nodes forming this graph.
 */
case class Graph private[framework](@(JsonValue@getter) nodes: Set[Node]) {
  private[this] val index = nodes.map(n => n.name -> n).toMap

  def size: Int = nodes.size

  def roots: Set[Node] = nodes.filter(_.isRoot)

  def leaves: Set[Node] = nodes.filter(_.isLeaf)

  def get(name: String): Option[Node] = index.get(name)

  @throws[NoSuchElementException]
  def apply(name: String): Node = index(name)

  def setParams(inputs: Map[Reference, Any]): Graph = {
    val newNodes = nodes.map { node =>
      val newInputs = inputs.filter(_._1.node == node.name).map { case (ref, value) => ref.port -> ValueInput(value) }
      node.copy(inputs = node.inputs ++ newInputs)
    }
    copy(nodes = newNodes)
  }
}

case class Node private[framework](
  name: String,
  op: String,
  inputs: Map[String, Input],
  outputs: Map[String, Set[Reference]]) extends Named {

  @JsonIgnore
  def isRoot: Boolean = inputs.values.forall {
    case _: ReferenceInput => false
    case _ => true
  }

  @JsonIgnore
  def isLeaf: Boolean = outputs.values.isEmpty

  def predecessors: Set[String] = dependencies.map(_.node)

  def dependencies: Set[Reference] = inputs.values.flatMap {
    case ReferenceInput(ref) => Some(ref)
    case _ => None
  }.toSet

  def successors: Set[String] = outputs.values.flatten.map(_.node).toSet
}

/**
 * Utils for [[Node]].
 */
object Node {
  /**
   * Pattern for valid node names.
   */
  val NamePattern = "[A-Z][a-zA-Z0-9_]+"

  /**
   * Regex for valid node names.
   */
  val NameRegex = ("^" + NamePattern + "$").r
}

@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[ValueInput], name = "value"),
  new JsonSubTypes.Type(value = classOf[ReferenceInput], name = "reference")))
@JsonIgnoreProperties(ignoreUnknown = true)
sealed trait Input

case class ValueInput(value: Any) extends Input with WrappedValue[Any]

case class ReferenceInput(reference: Reference) extends Input

/**
 * Definition of a graph. Definitions must be converted into [[Graph]]'s in order to be executed.
 *
 * @param nodes Definition of nodes forming this graph.
 */
case class GraphDef(@(JsonValue@getter) nodes: Seq[NodeDef])

case class NodeDef(
  op: String,
  @JsonProperty("name") customName: Option[String] = None,
  inputs: Map[String, Input] = Map.empty)
  extends Named {

  override def name: String = customName.getOrElse(op)
}