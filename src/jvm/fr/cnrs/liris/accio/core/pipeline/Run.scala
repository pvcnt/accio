package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path

import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.{DateTime, Duration}

/**
 * A run is a particular instantiation of a graph, where everything is well defined (i.e., all parameters are fixed and
 * have a single value). A run always belongs to an experiment.
 *
 * @param id       Unique identifier (among all runs AND experiments)
 * @param parent   Parent experiment identifier
 * @param name     Human-readable name
 * @param graphDef Graph being executed
 * @param report   Execution report
 */
case class Run(
  id: String,
  parent: String,
  graphDef: GraphDef,
  name: Option[String] = None,
  report: Option[RunReport] = None) {

  override def toString: String = name.getOrElse(id)
}

/**
 * Execution report of a run.
 *
 * @param startedAt   Time at which the execution started
 * @param completedAt Time at which the execution completed
 * @param nodeStats   Per-node stats
 * @param artifacts   Non-ephemeral artifacts
 */
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
   * Return a copy of this report with the execution of a given node marked as successfully completed.
   *
   * @param nodeName  Name of the node whose execution completed
   * @param artifacts Artifacts produced with the execution of the node
   * @param at        Time at which the node execution completed
   * @throws IllegalStateException If the node was not marked as started
   */
  @throws[IllegalStateException]
  def completeNode(nodeName: String, artifacts: Seq[Artifact], at: DateTime = DateTime.now): RunReport = {
    nodeStats.find(_.name.contains(nodeName)) match {
      case Some(s) =>
        copy(
          nodeStats = nodeStats - s + s.complete(at),
          artifacts = this.artifacts ++ artifacts.filterNot(_.ephemeral))
      case None => throw new IllegalStateException(s"Execution of node $nodeName was not started")
    }
  }

  /**
   * Return a copy of this report with the execution of a given node marked as errored.
   *
   * @param nodeName Name of the node whose execution completed
   * @param error    Error caught during execution
   * @param at       Time at which the node execution completed
   * @throws IllegalStateException If the node was not marked as started
   */
  @throws[IllegalStateException]
  def completeNode(nodeName: String, error: Throwable, at: DateTime = DateTime.now): RunReport = {
    nodeStats.find(_.name == nodeName) match {
      case Some(s) => copy(nodeStats = nodeStats - s + s.complete(error, at))
      case None => throw new IllegalStateException(s"Execution of node $nodeName was not started")
    }
  }

  /**
   * Return a copy of this report with the execution marked as completed.
   *
   * @param at Time at which the execution completed
   */
  def complete(at: DateTime = DateTime.now): RunReport = copy(completedAt = Some(at))
}

/**
 * Execution statistics for a node.
 *
 * @param name        Name of the node these stats apply to
 * @param startedAt   Time at which the execution started
 * @param completedAt Time at which the execution completed
 * @param successful  Did the execution completed successfully?
 * @param error       Error caught during execution
 */
case class NodeExecStats(
  name: String,
  startedAt: DateTime = DateTime.now,
  completedAt: Option[DateTime] = None,
  successful: Option[Boolean] = None,
  error: Option[Throwable] = None) {

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
   */
  def complete(at: DateTime = DateTime.now): NodeExecStats = copy(completedAt = Some(at), successful = Some(true))

  /**
   * Return completed statistics, after an error.
   *
   * @param error Error caught during execution
   */
  def complete(error: Throwable, at: DateTime = DateTime.now): NodeExecStats =
    copy(completedAt = Some(at), successful = Some(false), error = Some(error))
}

trait ReportWriter {
  def write(workDir: Path, experiment: ExperimentRun): Unit

  def write(workDir: Path, run: Run): Unit
}

trait ReportReader {
  def readExperiment(workDir: Path, id: String): ExperimentRun

  def readRun(workDir: Path, id: String): Run
}