package fr.cnrs.liris.accio.core.pipeline

import com.google.inject.Inject
import com.twitter.util.StorageUnit
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.dataset.{Dataset, DatasetEnv}
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.accio.core.model.Trace
import org.joda.time.Instant

import scala.collection.mutable

class GraphExecutor @Inject()(env: DatasetEnv) extends StrictLogging {
  def execute(graph: Graph): Report = {
    val startedAt = Instant.now()
    val outputs = mutable.Map.empty[String, Artifact]
    val nodeStats = mutable.Map.empty[String, ExecStats]
    val scheduled = mutable.Queue.empty[Node] ++ graph.roots
    while (scheduled.nonEmpty) {
      val node = scheduled.dequeue()
      logger.trace(s"Starting execution of node ${node.name}")
      val startedAt = Instant.now()
      val artifacts = run(graph, node, outputs.toMap)
      outputs ++= artifacts.map(art => art.name -> art)
      nodeStats(node.name) = ExecStats(startedAt, Instant.now(), successful = true, StorageUnit.zero)
      scheduled ++= node.successors.map(graph(_)).filter(_.dependencies.forall(outputs.contains))
      logger.trace(s"Completed execution of node ${node.name}")
    }
    val stats = ExecStats(startedAt, Instant.now(), successful = true, StorageUnit.zero)
    Report(stats, Map.empty, outputs.values.toSeq)
  }

  private def run(graph: Graph, node: Node, outputs: Map[String, Artifact]): Seq[Artifact] = {
    val inputs = node.dependencies.map { dep =>
      val node = graph(dep)
      outputs(node.name) match {
        case DatasetArtifact(_, data) => data
        //TODO: case StoredDatasetArtifact(_, path) =>
        case out => throw new RuntimeException(s"Cannot use $out as an input dependency")
      }
    }
    run(node.name, node.operator, inputs)
  }

  private def run(nodeName: String, operator: Operator, inputs: Seq[Dataset[Trace]]): Seq[Artifact] = {
    operator match {
      case s: Source => Seq(DatasetArtifact(nodeName, s.get(env)))
      case t: Transformer =>
        Seq(DatasetArtifact(nodeName, inputs.head.flatMap(t.transform)))
      case a: Analyzer =>
        val metrics = inputs.head.flatMap(a.analyze).toArray
        getDistributions(nodeName, metrics)
      case e: Evaluator =>
        val metrics = inputs.head.zip(inputs.last).flatMap { case (ref, res) => e.evaluate(ref, res) }.toArray
        getDistributions(nodeName, metrics)
    }
  }

  private def getDistributions(nodeName: String, metrics: Seq[Metric]) = {
    metrics.groupBy(_.name)
        .map { case (name, values) => DistributionArtifact(s"$nodeName/$name", values.map(_.value)) }
        .toSeq
  }
}