package fr.cnrs.liris.accio.core.pipeline

import fr.cnrs.liris.accio.core.framework.{Exploration, GraphDef, Report}

trait ExecutionStrategy {
  def next: Seq[GraphDef]

  def next(graph: GraphDef, report: Report): Seq[GraphDef]
}

class SingleExecutionStrategy(graph: GraphDef) extends ExecutionStrategy {
  override def next: Seq[GraphDef] = Seq(graph)

  override def next(graph: GraphDef, report: Report): Seq[GraphDef] = Seq.empty
}

class ExplorationStrategy(graph: GraphDef, exploration: Exploration) extends ExecutionStrategy {
  override def next: Seq[GraphDef] = exploration.paramGrid.toSeq.map(graph.set)

  override def next(graph: GraphDef, report: Report): Seq[GraphDef] = Seq.empty
}