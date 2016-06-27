package fr.cnrs.liris.accio.cli

import fr.cnrs.liris.accio.core.pipeline.{Artifact, WorkflowRun}

class ReportStatistics(val runs: Seq[WorkflowRun]) {
  /**
   * Return all artifacts as a map with names as keys and artifacts keyed by run name/id as value.
   */
  def artifacts: Map[String, Map[String, Artifact]] = runs
      .flatMap(run => run.report.artifacts.map(art => run.name.getOrElse(run.id) -> art))
      .groupBy(_._2.name)
      .map { case (id, group) => id -> group.toMap }
}