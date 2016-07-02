package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path

import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.{DateTime, Duration}

case class ExperimentRun(
  id: String,
  defn: ExperimentDef,
  stats: ExecStats = new ExecStats,
  children: Seq[String] = Seq.empty) {

  def complete(successful: Boolean): ExperimentRun = copy(stats = stats.complete(successful))
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
case class Run(
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
  nodeStats: Set[ExecStats] = Set.empty,
  artifacts: Set[Artifact] = Set.empty) {

  def start(nodeName: String): Report = copy(nodeStats = nodeStats + new ExecStats(Some(nodeName)))

  def complete(nodeName: String, artifacts: Seq[Artifact]): Report = {
    nodeStats.find(_.name.contains(nodeName)) match {
      case Some(s) =>
        copy(
          nodeStats = nodeStats - s + s.complete(successful = true),
          artifacts = this.artifacts ++ artifacts.filterNot(_.ephemeral))
      case None => throw new IllegalStateException(s"Execution of node $nodeName was not started")
    }
  }

  def complete(nodeName: String, error: Throwable): Report = {
    nodeStats.find(_.name == nodeName) match {
      case Some(s) => copy(nodeStats = nodeStats - s + s.complete(error))
      case None => throw new IllegalStateException(s"Execution of node $nodeName was not started")
    }
  }

  def complete(successful: Boolean): Report = copy(stats = stats.complete(successful))

  def complete(e: Throwable): Report = copy(stats = stats.complete(e))
}

/**
 * Execution statistics.
 *
 * @param name        Name of the node these stats apply to
 * @param startedAt   Time at which the execution started
 * @param completedAt Time at which the execution completed
 * @param successful  Did the execution completed successfully?
 * @param error       Error caught during execution
 */
case class ExecStats(
  name: Option[String] = None,
  startedAt: DateTime = DateTime.now,
  completedAt: Option[DateTime] = None,
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
  def complete(successful: Boolean): ExecStats = copy(completedAt = Some(DateTime.now), successful = Some(successful))

  /**
   * Return completed statistics, after an error.
   *
   * @param error Error caught during execution
   */
  def complete(error: Throwable): ExecStats =
    copy(completedAt = Some(DateTime.now), successful = Some(false), error = Some(error))
}

trait ReportWriter {
  def write(workDir: Path, experiment: ExperimentRun): Unit

  def write(workDir: Path, run: Run): Unit
}

trait ReportReader {
  def readExperiment(workDir: Path, id: String): ExperimentRun

  def readRun(workDir: Path, id: String): Run
}