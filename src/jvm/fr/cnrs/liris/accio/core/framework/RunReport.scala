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

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}
import fr.cnrs.liris.common.util.Named
import org.joda.time.{DateTime, Duration}

/**
 * Execution report of a run.
 *
 * @param startedAt   Time at which the execution started
 * @param completedAt Time at which the execution completed
 * @param nodeStats   Per-node stats
 * @param artifacts   Non-ephemeral artifacts
 */
@JsonIgnoreProperties(ignoreUnknown = true)
case class RunReport(
  startedAt: DateTime = DateTime.now,
  completedAt: Option[DateTime] = None,
  nodeStats: Set[NodeExecStats] = Set.empty,
  artifacts: Set[Artifact] = Set.empty) {

  /**
   * Return whether the execution is completed, either successfully or not.
   */
  @JsonProperty
  def completed: Boolean = completedAt.nonEmpty

  /**
   * Return whether the execution is successful so far, i.e., if all nodes completed successfully.
   */
  @JsonProperty
  def successful: Boolean = nodeStats.forall(_.successful.getOrElse(true))

  /**
   * Return the execution duration.
   */
  @JsonProperty
  def duration: Option[Duration] = completedAt.map(end => Duration.millis(end.getMillis - startedAt.getMillis))

  /**
   * Return a copy of this report with the execution of a given node marked as started.
   *
   * @param nodeName Name of the node whose execution started
   * @param at       Time at which the node execution started
   */
  def startNode(nodeName: String, at: DateTime = DateTime.now): RunReport =
  copy(nodeStats = nodeStats + new NodeExecStats(nodeName, at))

  /**
   * Return a copy of this report with the execution of a given node marked as successfully
   * completed.
   *
   * @param nodeName  Name of the node whose execution completed
   * @param artifacts Artifacts produced with the execution of the node
   * @param at        Time at which the node execution completed
   * @throws IllegalStateException If the node was not marked as started
   */
  @throws[IllegalStateException]
  def completeNode(nodeName: String, artifacts: Seq[Artifact], at: DateTime): RunReport =
  nodeStats.find(_.name.contains(nodeName)) match {
    case Some(s) => copy(
      nodeStats = nodeStats - s + s.complete(at),
      artifacts = this.artifacts ++ artifacts)
    case None => throw new IllegalStateException(s"Execution of node $nodeName was not started")
  }

  /**
   * Return a copy of this report with the execution of a given node marked as successfully
   * completed, considering it completed just now.
   *
   * @param nodeName  Name of the node whose execution completed
   * @param artifacts Artifacts produced with the execution of the node
   * @throws IllegalStateException If the node was not marked as started
   */
  @throws[IllegalStateException]
  def completeNode(nodeName: String, artifacts: Seq[Artifact]): RunReport =
  completeNode(nodeName, artifacts, DateTime.now)

  /**
   * Return a copy of this report with the execution of a given node marked as errored.
   *
   * @param nodeName Name of the node whose execution completed
   * @param error    Error caught during execution
   * @param at       Time at which the node execution completed
   * @throws IllegalStateException If the node was not marked as started
   */
  @throws[IllegalStateException]
  def completeNode(nodeName: String, error: Throwable, at: DateTime): RunReport =
  nodeStats.find(_.name == nodeName) match {
    case Some(s) => copy(nodeStats = nodeStats - s + s.complete(error, at))
    case None => throw new IllegalStateException(s"Execution of node $nodeName was not started")
  }

  /**
   * Return a copy of this report with the execution of a given node marked as errored, considering
   * it completed just now.
   *
   * @param nodeName Name of the node whose execution completed
   * @param error    Error caught during execution
   * @throws IllegalStateException If the node was not marked as started
   */
  @throws[IllegalStateException]
  def completeNode(nodeName: String, error: Throwable): RunReport =
  completeNode(nodeName, error, DateTime.now)

  /**
   * Return a copy of this report with the execution marked as completed.
   *
   * @param at Time at which the execution completed
   */
  def complete(at: DateTime = DateTime.now): RunReport = copy(completedAt = Some(at))
}

/**
 *
 * @param name
 * @param kind
 * @param value
 */
case class Artifact(name: String, @JsonProperty("type") kind: DataType, value: Any) extends Named

/**
 * Execution statistics for a node.
 *
 * @param name        Name of the node these stats apply to
 * @param startedAt   Time at which the execution started
 * @param completedAt Time at which the execution completed
 * @param successful  Did the execution completed successfully?
 * @param error       Error caught during execution
 */
@JsonIgnoreProperties(ignoreUnknown = true)
case class NodeExecStats(
  name: String,
  startedAt: DateTime = DateTime.now,
  completedAt: Option[DateTime] = None,
  successful: Option[Boolean] = None,
  error: Option[ErrorDatum] = None) {

  /**
   * Return whether the execution is completed, either successfully or not.
   */
  @JsonProperty
  def completed: Boolean = completedAt.nonEmpty

  /**
   * Return the execution duration.
   */
  @JsonProperty
  def duration: Option[Duration] = completedAt.map(end => Duration.millis(end.getMillis - startedAt.getMillis))

  /**
   * Return successfully completed statistics.
   *
   * @param at Time at which the node execution completed
   */
  def complete(at: DateTime = DateTime.now): NodeExecStats = copy(completedAt = Some(at), successful = Some(true))

  /**
   * Return successfully completed statistics, considering it completed just now.
   */
  def complete(): NodeExecStats = complete(DateTime.now)

  /**
   * Return completed statistics, after an error.
   *
   * @param error Error caught during execution
   * @param at    Time at which the node execution completed
   */
  def complete(error: Throwable, at: DateTime): NodeExecStats =
  copy(completedAt = Some(at), successful = Some(false), error = Some(ErrorDatum(error)))

  /**
   * Return completed statistics, after an error, considering it completed just now.
   *
   * @param error Error caught during execution
   */
  def complete(error: Throwable): NodeExecStats = complete(error, DateTime.now)
}