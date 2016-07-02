package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path

import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.{Duration, Instant}

case class ExperimentRun(
  id: String,
  defn: ExperimentDef,
  stats: ExecStats = new ExecStats(Instant.now),
  children: Seq[String] = Seq.empty) {

  def complete(successful: Boolean): ExperimentRun =
    copy(stats = stats.copy(completedAt = Some(Instant.now), successful = Some(successful)))
}

/**
 * A run is a particular instantiation of a graph, where everything is well defined (i.e., all
 * parameters are fixed and have a single value). A run always belongs to an experiment.
 *
 * @param id       Run unique identifier
 * @param parent   Parent experiment identifier
 * @param name     Human-readable name
 * @param graphDef Graph being executed
 * @param report   Execution report
 */
case class WorkflowRun(
  id: String,
  parent: String,
  graphDef: GraphDef,
  name: Option[String] = None,
  report: Report = new Report) {

  override def toString: String = name.getOrElse(id)
}

/**
 * Execution report of a workflow run.
 *
 * @param stats     Global stats of the whole run
 * @param nodeStats Per-node stats
 * @param artifacts Non-ephemeral artifacts produced
 */
case class Report(
  stats: ExecStats = new ExecStats,
  nodeStats: Map[String, ExecStats] = Map.empty,
  artifacts: Seq[Artifact] = Seq.empty) {

  def start(nodeName: String): Report = copy(nodeStats = nodeStats.updated(nodeName, new ExecStats))

  def complete(nodeName: String, artifacts: Seq[Artifact]): Report = {
    val stats = nodeStats(nodeName)
    copy(
      nodeStats = nodeStats.updated(nodeName, stats.complete(successful = true)),
      artifacts = this.artifacts ++ artifacts.filterNot(_.ephemeral))
  }

  def complete(nodeName: String, error: Throwable): Report = {
    val stats = nodeStats(nodeName)
    copy(nodeStats = nodeStats.updated(nodeName, stats.complete(error)))
  }

  def complete(successful: Boolean): Report = copy(stats = stats.complete(successful))

  def complete(e: Throwable): Report = copy(stats = stats.complete(e))
}

/**
 * Execution statistics.
 *
 * @param startedAt   Time at which the execution started
 * @param completedAt Time at which the execution completed
 * @param successful  Did the execution completed successfully?
 * @param error       Error caught during execution
 */
case class ExecStats(
  startedAt: Instant = Instant.now,
  completedAt: Option[Instant] = None,
  successful: Option[Boolean] = None,
  error: Option[Throwable] = None) {

  /**
   * Return the execution duration.
   */
  @JsonProperty
  def duration: Option[Duration] = completedAt.map(end => Duration.millis(end.getMillis - startedAt.getMillis))

  /**
   * Return completed statistics, either successfully or not.
   *
   * @param successful Did the execution completed successfully?
   */
  def complete(successful: Boolean): ExecStats = copy(completedAt = Some(Instant.now), successful = Some(successful))

  /**
   * Return completed statistics, after an error.
   *
   * @param error Error caught during execution
   */
  def complete(error: Throwable): ExecStats =
    copy(completedAt = Some(Instant.now), successful = Some(false), error = Some(error))
}

trait ReportWriter {
  def write(workDir: Path, experiment: ExperimentRun): Unit

  def write(workDir: Path, run: WorkflowRun): Unit
}

trait ReportReader {
  def readExperiment(workDir: Path, id: String): ExperimentRun

  def readRun(workDir: Path, id: String): WorkflowRun
}