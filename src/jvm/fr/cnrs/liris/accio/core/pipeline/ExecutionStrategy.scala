package fr.cnrs.liris.accio.core.pipeline

import fr.cnrs.liris.accio.core.framework.{Exploration, GraphDef, Report}

trait ExecutionStrategy {
  def next: Seq[(GraphDef, Any)]

  def next(graphDef: GraphDef, meta: Any, report: Report): Seq[(GraphDef, Any)]
}

class SingleExecutionStrategy(graph: GraphDef) extends ExecutionStrategy {
  override def next: Seq[(GraphDef, Any)] = Seq(graph -> None)

  def next(graphDef: GraphDef, meta: Any, report: Report): Seq[(GraphDef, Any)] = Seq.empty
}

class ExplorationStrategy(graph: GraphDef, exploration: Exploration) extends ExecutionStrategy {
  override def next: Seq[(GraphDef, Any)] = exploration.paramGrid.toSeq.map(graph.set).map(g => g -> None)

  def next(graphDef: GraphDef, meta: Any, report: Report): Seq[(GraphDef, Any)] = Seq.empty
}