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

import fr.cnrs.liris.accio.api.thrift.{Channel, NamedChannel, Reference}
import fr.cnrs.liris.util.Seqs

/**
 * An executable graph.
 *
 * @param steps Steps forming this graph.
 */
final class Graph private[accio](val steps: Seq[Step] = Seq.empty) {
  private[this] val index = steps.map(step => step.name -> step).toMap

  /**
   * Return the set of root steps.
   */
  def roots: Set[Step] = steps.filter(_.isRoot).toSet

  /**
   * Return a step by its name, if it exists.
   *
   * @param name Step name.
   */
  def get(name: String): Option[Step] = index.get(name)

  /**
   * Return a step by its name.
   *
   * @param name Step name.
   * @throws NoSuchElementException If thee is no step with given name.
   */
  def apply(name: String): Step = index(name)

  /**
   * Convert this graph into an equivalent Thrift structure.
   */
  def toThrift: Seq[thrift.Step] = steps.map(_.toThrift)
}

/**
 * Factory for [[Graph]].
 */
object Graph {
  /**
   * Create a graph from an equivalent Thrift structure.
   *
   * @param steps Graph, as a Thrift structure.
   */
  def fromThrift(steps: Seq[thrift.Step]): Graph = {
    // First create the steps without specifying any output.
    val stepsMap = steps.map(step => step.name -> Step.fromThrift(step))

    // We now connect steps together. Input dependencies are already defined when creating the
    // step, we only have to wire output dependencies correctly.
    new Graph(stepsMap.map(_._2).map(wire(_, stepsMap.toMap)))
  }

  /**
   * Create connections between outputs of a step and all other steps consuming them.
   *
   * @param step     Step to connect.
   * @param stepsMap All steps inside the graph, indexed by name.
   * @return Updated step, with outputs connected.
   */
  private def wire(step: Step, stepsMap: Map[String, Step]): Step = {
    val outputs = Seqs.index(stepsMap.values.flatMap { otherNode =>
      otherNode.inputs.flatMap {
        case NamedChannel(otherPort, Channel.Reference(ref)) =>
          if (ref.step == step.name) {
            Some(ref.output -> thrift.Reference(otherNode.name, otherPort))
          } else {
            None
          }
        case _ => None
      }
    }.toSet)
    step.copy(outputs = outputs)
  }
}

/**
 * A step inside a graph. A step is a particular instantiation of an operator, with given inputs.
 *
 * @param name    Step name.
 * @param op      Operator name.
 * @param inputs  Inputs of the operator.
 * @param exports Exports.
 * @param outputs Outputs of the operator, i.e., references to ports of other steps where outputs are consumed.
 */
case class Step private[accio](
  name: String,
  op: String,
  inputs: Seq[thrift.NamedChannel],
  exports: Seq[thrift.Export],
  outputs: Map[String, Set[thrift.Reference]]) {

  /**
   * Check whether this step is a root step, i.e., it does not depend on any other step.
   */
  def isRoot: Boolean =
    inputs.map(_.channel).forall {
      case _: Channel.Reference => false
      case _ => true
    }

  /**
   * Return the predecessors of this step, i.e., steps that this step depends on.
   *
   * @return List of step names.
   */
  def predecessors: Set[String] =
    inputs.map(_.channel).flatMap {
      case Channel.Reference(Reference(step, _)) => Some(step)
      case _ => None
    }.toSet

  /**
   * Return the successors of this step, i.e., other steps that depend on this step.
   *
   * @return List of step names.
   */
  def successors: Set[String] = outputs.values.flatten.map(_.step).toSet

  /**
   * Convert this step into an equivalent Thrift structure.
   */
  private[api] def toThrift: thrift.Step = thrift.Step(op, name, inputs, exports)
}

/**
 * Factory for [[Step]].
 */
object Step {
  /**
   * Create a step from en equivalent Thrift structure.
   *
   * @param struct Step, as a Thrift structure.
   */
  private[api] def fromThrift(struct: thrift.Step): Step = {
    Step(struct.name, struct.op, struct.inputs, struct.exports, Map.empty)
  }
}