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

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.base.MoreObjects
import com.twitter.finatra.domain.WrappedValue
import fr.cnrs.liris.common.util.HashUtils
import org.joda.time.{DateTime, Duration}

/**
 * A run is a particular instantiation of a workflow, where everything is well defined (i.e., all workflow parameters
 * have been affected a value).
 *
 * Runs are executed on a given cluster by a given user and inside a given environment. The user is used for
 * accounting and quotas, the environment for priority-based scheduling. The environment can have one of the
 * following values (by ascending priority): "devel", "test", "staging", "production".
 *
 * Runs can be single runs, cloned from another run or belong to a parameter sweep. Clones have their `clonedFrom`
 * property defined to the identifier of the run they have been cloned from, and with which they share the same
 * workflow, cluster and environment. Runs that are part of a parameter sweep have their `parent` property set to the
 * identifier of the run that acts as a root for the parameter sweep. All runs that are part of a same parameter sweep
 * share the same workflow, cluster, environment, user and metadata. Parent runs are "dummy" runs that are not
 * actually scheduled, although their [[RunStatus]] will be updated as the execution of their children progresses.
 *
 * Runs are mostly immutable, only the `status`, `name`, `notes` and `tags` properties are designed to be updated
 * after a run has been created.
 *
 * @param id          Run unique identifier.
 * @param pkg         Description of the workflow.
 * @param cluster     Cluster on which the run is executed.
 * @param owner       User that initiated the run.
 * @param environment Environment inside which the run is executed.
 * @param name        Human-readable name.
 * @param notes       Notes describing the purpose of the run.
 * @param tags        Arbitrary tags helping with run classification.
 * @param seed        Seed used by unstable operators.
 * @param params      Values of workflow parameters.
 * @param parent      Identifier of the run this instance is a child of.
 * @param children    Identifiers of the runs this instance is a parent of.
 * @param clonedFrom  Identifier of the run this instance has been cloned from.
 * @param createdAt   Time at which this run has been created.
 * @param status      Execution status.
 */
case class Run(
  id: RunId,
  @JsonProperty("package") pkg: Package,
  cluster: String,
  owner: User,
  environment: String,
  name: String,
  notes: Option[String],
  tags: Set[String],
  seed: Long,
  params: Map[String, Any],
  parent: Option[RunId],
  children: Set[RunId],
  clonedFrom: Option[RunId],
  createdAt: DateTime,
  status: RunStatus) {

  def updated(fn: RunStatus => RunStatus): Run = synchronized {
    copy(status = fn(status))
  }

  override def equals(other: Any): Boolean = other match {
    case r: Run => r.id == id
    case _ => false
  }

  override def hashCode: Int = id.hashCode

  override def toString: String =
    MoreObjects.toStringHelper(this)
      .add("id", id)
      .toString
}

/**
 * Helpers for [[Run]]s.
 */
object Run {
  val DefaultEnvironment = "devel"
  val DefaultCluster = "default"

  /**
   * Generate a human-readable label for a list of parameters.
   *
   * @param params List of parameters.
   */
  def label(params: Seq[(String, Any)]): String = {
    params.map { case (k, v) =>
      var vStr = v.toString
      if (vStr.contains('/')) {
        // Remove any slash that would be polluting directory name.
        vStr = vStr.substring(vStr.lastIndexOf('/') + 1)
      }
      s"$k=$vStr"
    }.mkString(",")
  }

  /**
   * Generate a human-readable label for a map of parameters.
   *
   * @param params Map of parameters.
   */
  def label(params: Map[String, Any]): String = label(params.toSeq)
}

/**
 * Run identifier.
 *
 * @param value String value.
 */
case class RunId(value: String) extends WrappedValue[String] {
  def shorten: RunId = RunId(value.substring(0, 8))
}

/**
 * Factory for [[RunId]].
 */
object RunId {
  /**
   * Create a random run identifier.
   */
  def random: RunId = RunId(UUID.randomUUID().toString)
}

/**
 * Describe the executed workflow. It is quite complex as two things need to be taken into account, the workflow
 * (of course) and the Java code of the operators that are part of the workflow.
 *
 * For the workflow, things are pretty easy as workflows are stored inside a repository and versioned, which gives
 * us traceability and unicity of the graph represented by a (workflow id, workflow version) pair. For the Java code
 * things are more complicated because new code for an operator can be potentially deployed at anytime, thus
 * possibly altering the reproducibility of experiments. To mitigate this, we track as part of the package, for each
 * operator used in the target workflow, a fingerprint of the operator.
 *
 * @param workflowId      Workflow identifier.
 * @param workflowVersion Workflow version.
 */
case class Package(workflowId: WorkflowId, workflowVersion: Int)

/**
 * Describe the current execution status of a run. Status is updated as the execution progresses. Please note that
 * this class does not contain directly the outcome of nodes execution, only a reference to keys used to query it.
 * The goal is to avoid having [[Run]] objects of uncontrolled size (as the number of artifacts depends on the
 * workflow and the size of each artifact depends on the underlying data). Execution status of nodes are stored in
 * [[NodeStatus]] instances, that are persisted independently into their own [[ArtifactRepository]].
 *
 * For parent runs, the `nodes` property has to stay empty, the `progress` property should indicate the percentage
 * of completed child runs and the `successful` property whether all children completed successfully. Otherwise,
 * the `progress` property should indicate the percentage of completed nodes and the `successful` property whether
 * all operators completed successfully.
 *
 * @param startedAt      Time at which the execution started.
 * @param progress       Execution progress (percentage, between 0 and 1).
 * @param completedAt    Time at which the execution completed.
 * @param successful     Did the execution completed successfully?
 * @param runningNodes   Mapping between running node names and time at which they started.
 * @param completedNodes Mapping between node names and keys to query their status.
 */
case class RunStatus private(
  startedAt: Option[DateTime],
  progress: Double,
  completedAt: Option[DateTime],
  successful: Option[Boolean],
  runningNodes: Map[String, DateTime],
  completedNodes: Map[String, NodeKey]) {

  require(progress >= 0 && progress <= 1, s"Progress must be in [0,1], got: $progress")

  /**
   * Return whether the execution is completed, either successfully or not.
   */
  @JsonProperty("completed")
  def isCompleted: Boolean = completedAt.nonEmpty

  /**
   * Return the execution duration.
   */
  @JsonProperty
  def duration: Option[Duration] = completedAt.map(end => Duration.millis(end.getMillis - startedAt.get.getMillis))

  /**
   * Return a copy of this instance, with the execution marked as started.
   */
  def started: RunStatus = synchronized {
    startedAt match {
      case Some(_) => throw new IllegalStateException("Execution of run is already started")
      case None => copy(startedAt = Some(DateTime.now))
    }
  }

  /**
   * Return a copy of this instance, with the execution of a given node marked as started.
   *
   * @param nodeName Name of the node whose execution started.
   */
  def nodeStarted(nodeName: String): RunStatus = synchronized {
    runningNodes.get(nodeName) match {
      case Some(_) => throw new IllegalStateException(s"Node $nodeName is already started")
      case None => copy(runningNodes = runningNodes + (nodeName -> DateTime.now))
    }
  }

  /**
   * Return a copy of this instance, with the execution of a given node marked as completed.
   *
   * @param nodeName Name of the node whose execution completed.
   * @param nodeKey  Key to query node status.
   * @param progress Execution progress (percentage, between 0 and 1).
   */
  def nodeCompleted(nodeName: String, nodeKey: NodeKey, progress: Double): RunStatus = synchronized {
    runningNodes.get(nodeName) match {
      case None => throw new IllegalStateException(s"Node $nodeName was not started")
      case Some(_) => copy(
        runningNodes = runningNodes - nodeName,
        completedNodes = completedNodes + (nodeName -> nodeKey),
        progress = progress)
    }
  }

  /**
   * Return a copy of this instance, with the execution marked as completed.
   *
   * @param successful Did the execution completed successfully?
   */
  def completed(successful: Boolean): RunStatus = synchronized {
    completedAt match {
      case Some(_) => throw new IllegalStateException("Execution of run is already completed")
      case None => copy(completedAt = Some(DateTime.now), successful = Some(successful))
    }
  }
}

/**
 * Factory for [[RunStatus]].
 */
object RunStatus {
  def empty: RunStatus = RunStatus(None, 0, None, None, Map.empty, Map.empty)
}