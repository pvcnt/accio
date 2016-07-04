package fr.cnrs.liris.accio.core.pipeline

import com.fasterxml.jackson.annotation.JsonProperty
import fr.cnrs.liris.accio.core.param.ParamMap
import org.joda.time.Instant

trait ExecutionStrategy {
  def next: Seq[(GraphDef, Any)]

  def next(graphDef: GraphDef, meta: Any, report: RunReport): Seq[(GraphDef, Any)]

  def name(graphDef: GraphDef): Option[String]
}

class SingleExecutionStrategy(graph: GraphDef) extends ExecutionStrategy {
  override def next: Seq[(GraphDef, Any)] = Seq(graph -> None)

  override def next(graphDef: GraphDef, meta: Any, report: RunReport): Seq[(GraphDef, Any)] = Seq.empty

  override def name(graphDef: GraphDef): Option[String] = None
}

class ExplorationStrategy(graph: GraphDef, exploration: Exploration) extends ExecutionStrategy {
  override def next: Seq[(GraphDef, Any)] =
    exploration.paramGrid.toSeq.map(graph.setParams).map(g => g -> None)

  override def next(graphDef: GraphDef, meta: Any, report: RunReport): Seq[(GraphDef, Any)] = Seq.empty

  override def name(graphDef: GraphDef): Option[String] = {
    Some(graphDef.params.filter(exploration.paramGrid.keys).toString)
  }
}

case class Observation(paramMap: ParamMap, report: Option[RunReport], createdAt: Instant) {
  @JsonProperty
  def failed: Boolean = report.isEmpty
}

object Observation {
  def apply(paramMap: ParamMap, report: RunReport): Observation =
    new Observation(paramMap, Some(report), Instant.now)

  def failed(paramMap: ParamMap): Observation = new Observation(paramMap, None, Instant.now)
}