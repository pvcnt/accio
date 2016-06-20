package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path

import com.twitter.util.StorageUnit
import org.joda.time.Instant

case class Experiment(
    id: String,
    defn: ExperimentDef,
    children: Seq[String] = Seq.empty,
    stats: Option[ExecStats] = None)

case class Report(stats: ExecStats, nodes: Map[String, ExecStats], artifacts: Seq[Artifact]) {
  def withoutEphemeral: Report = copy(artifacts = artifacts.filterNot(_.ephemeral))
}

case class ExecStats(startedAt: Instant, completedAt: Instant, successful: Boolean, memoryPeak: StorageUnit)

case class WorkflowRun(id: String, name: Option[String], graphDef: GraphDef, report: Option[Report])

trait ExperimentWriter {
  def write(workDir: Path, experiment: Experiment): Unit

  def write(workDir: Path, run: WorkflowRun): Unit
}

trait ExperimentReader {
  def readExperiment(workDir: Path, id: String): Experiment

  def readWorkflowRun(workDir: Path, id: String): WorkflowRun
}