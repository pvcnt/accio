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

package fr.cnrs.liris.accio.domain

/**
 * An executable graph.
 *
 * @param nodes Nodes forming this graph.
 */
private[accio] class Graph(nodes: Map[String, Graph.Node]) {
  /**
   * Return the set of root steps.
   */
  def roots: Set[Graph.Node] = nodes.values.filter(_.isRoot).toSet

  /**
   * Return a step by its name.
   *
   * @param name Step name.
   * @throws NoSuchElementException If there is no step with given name.
   */
  def apply(name: String): Graph.Node = nodes(name)
}

private[accio] object Graph {
  def create(workflow: Workflow): Graph = {
    val nodes = workflow.steps.map { step =>
      val predecessors = step.params.map(_.source).flatMap {
        case Channel.Reference(stepName, _) => Set(stepName)
        case _ => Set.empty[String]
      }.toSet
      val successors = workflow.steps.flatMap { other =>
        other.params.flatMap {
          case Channel(_, Channel.Reference(stepName, _)) if stepName == step.name => Set(other.name)
          case _ => Set.empty[String]
        }
      }.toSet
      step.name -> Node(step.name, predecessors, successors)
    }
    new Graph(nodes.toMap)
  }

  case class Node(name: String, predecessors: Set[String], successors: Set[String]) {
    /**
     * Check whether this step is a root node, i.e., it does not depend on any other node.
     */
    def isRoot: Boolean = predecessors.isEmpty
  }

}