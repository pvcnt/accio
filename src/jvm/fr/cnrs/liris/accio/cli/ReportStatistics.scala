package fr.cnrs.liris.accio.cli

import fr.cnrs.liris.accio.core.pipeline.{Artifact, GraphDef, Run}

import scala.collection.mutable

class ReportStatistics(val runs: Seq[Run]) {
  require(runs.nonEmpty, "You must provide some runs to aggregate")

  def similarGraphs: Seq[Seq[GraphDef]] = {
    val handled = mutable.Set.empty[Int]
    runs.map(_.graph).zipWithIndex.map { case (graphDef, idx) =>
      val similar = runs.zipWithIndex.filter(p => !handled.contains(p._2) && p._1.graph.hasSameStructure(graphDef))
      handled ++= similar.map(_._2)
      similar.map(_._1.graph)
    }
  }

  /**
   * Return all artifacts as a map with names as keys and artifacts keyed by run name/id as value.
   */
  def artifacts: Map[String, Map[String, Artifact]] =
    runs.flatMap { run => run.report.map(_.artifacts.map(art => run.name.getOrElse(run.id) -> art)).getOrElse(Seq.empty[(String, Artifact)]) }
        .groupBy(_._2.name)
        .map { case (id, group) => id -> group.toMap }
}