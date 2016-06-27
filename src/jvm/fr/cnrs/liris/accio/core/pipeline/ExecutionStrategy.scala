package fr.cnrs.liris.accio.core.pipeline

trait ExecutionStrategy {
  def next: Seq[(GraphDef, Any)]

  def next(graphDef: GraphDef, meta: Any, report: Report): Seq[(GraphDef, Any)]

  def name(graphDef: GraphDef): Option[String]
}

class SingleExecutionStrategy(graph: GraphDef) extends ExecutionStrategy {
  override def next: Seq[(GraphDef, Any)] = Seq(graph -> None)

  override def next(graphDef: GraphDef, meta: Any, report: Report): Seq[(GraphDef, Any)] = Seq.empty

  override def name(graphDef: GraphDef): Option[String] = None
}

class ExplorationStrategy(graph: GraphDef, exploration: Exploration) extends ExecutionStrategy {
  override def next: Seq[(GraphDef, Any)] =
    exploration.paramGrid.toSeq.map(graph.setParams).map(g => g -> None)

  override def next(graphDef: GraphDef, meta: Any, report: Report): Seq[(GraphDef, Any)] = Seq.empty

  override def name(graphDef: GraphDef): Option[String] = {
    Some(graphDef.params.filter(exploration.paramGrid.keys).toString)
  }
}