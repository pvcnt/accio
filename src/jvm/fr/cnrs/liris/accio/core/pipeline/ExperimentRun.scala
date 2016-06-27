package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path

import org.joda.time.Instant

case class ExperimentRun(
    id: String,
    defn: ExperimentDef,
    stats: ExecStats = new ExecStats(Instant.now),
    children: Seq[String] = Seq.empty) {

  def complete(successful: Boolean): ExperimentRun =
    copy(stats = stats.copy(completedAt = Some(Instant.now), successful = Some(successful)))
}

case class Report(
    stats: ExecStats = new ExecStats(Instant.now),
    nodeStats: Map[String, ExecStats] = Map.empty,
    artifacts: Seq[Artifact] = Seq.empty) {

  def start(nodeName: String): Report = {
    copy(nodeStats = nodeStats.updated(nodeName, new ExecStats(Instant.now)))
  }

  def complete(nodeName: String, successful: Boolean): Report = {
    val stats = nodeStats(nodeName)
    copy(nodeStats = nodeStats.updated(nodeName, stats.copy(completedAt = Some(Instant.now), successful = Some(true))))
  }

  def complete(successful: Boolean): Report = copy(
    artifacts = artifacts.filterNot(_.ephemeral),
    stats = stats.copy(completedAt = Some(Instant.now), successful = Some(successful)))
}

case class ExecStats(
    startedAt: Instant,
    completedAt: Option[Instant] = None,
    successful: Option[Boolean] = None)

/**
 * A run is a particular instantiation of a graph, where everything is well defined (i.e., all
 * parameters are fixed and have a single value). A run always belong to an experiment.
 *
 * @param id       Run unique identifier
 * @param parent   Parent experiment identifier
 * @param name     Human-readable name
 * @param graphDef Graph being executed
 * @param report
 */
case class WorkflowRun(
    id: String,
    parent: String,
    graphDef: GraphDef,
    name: Option[String] = None,
    report: Report = new Report)

trait ReportWriter {
  def write(workDir: Path, experiment: ExperimentRun): Unit

  def write(workDir: Path, run: WorkflowRun): Unit
}

trait ReportReader {
  def readExperiment(workDir: Path, id: String): ExperimentRun

  def readRun(workDir: Path, id: String): WorkflowRun
}