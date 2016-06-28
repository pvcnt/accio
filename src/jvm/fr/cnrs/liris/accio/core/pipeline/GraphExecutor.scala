package fr.cnrs.liris.accio.core.pipeline

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.dataset.{Dataset, DatasetEnv}
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.common.util.Requirements._
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
      nodeStats(node.name) = ExecStats(startedAt, Some(Instant.now()), successful = Some(true))
      scheduled ++= node.successors.map(graph(_)).filter(_.dependencies.forall(outputs.contains))
      logger.trace(s"Completed execution of node ${node.name}")
    }
    val stats = ExecStats(startedAt, Some(Instant.now()), successful = Some(true))
    Report(stats, nodeStats.toMap, outputs.values.toSeq)
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
    run(node.name, node.runs, node.operator, inputs)
  }

  private def run(nodeName: String, runs: Int, operator: Operator, inputs: Seq[Dataset[Trace]]): Seq[Artifact] = {
    operator match {
      case s: Source => run(s, nodeName, inputs)
      case t: Transformer => run(t, nodeName, inputs)
      case a: Analyzer => run(a, nodeName, runs, inputs)
      case e: Evaluator => run(e, nodeName, runs, inputs)
    }
  }

  private def run(source: Source, nodeName: String, inputs: Seq[Dataset[Trace]]) = {
    requireState(inputs.isEmpty, s"Source requires no input (got ${inputs.size})")
    Seq(DatasetArtifact(nodeName, source.get(env)))
  }

  private def run(transformer: Transformer, nodeName: String, inputs: Seq[Dataset[Trace]]) = {
    requireState(inputs.size == 1, s"Transformer requires exactly one input (got ${inputs.size})")
    Seq(DatasetArtifact(nodeName, inputs.head.flatMap(transformer.transform)))
  }

  private def run(analyzer: Analyzer, nodeName: String, runs: Int, inputs: Seq[Dataset[Trace]]) = {
    requireState(inputs.size == 1, s"Analyzer requires exactly one input (got ${inputs.size})")
    val metrics = Array.fill(runs) {
      inputs.head.flatMap(trace => analyzer.analyze(trace).map(metric => (trace.user, metric))).toArray
    }.flatten
    getDistributions(nodeName, metrics)
  }

  private def run(evaluator: Evaluator, nodeName: String, runs: Int, inputs: Seq[Dataset[Trace]]) = {
    requireState(inputs.size == 2, s"Evaluator requires exactly two inputs (got ${inputs.size})")
    val metrics = inputs.head.zip(inputs.last).flatMap { case (ref, res) =>
      require(ref.user == res.user, s"Trace mismatch: ${ref.user} / ${res.user}")
      Seq.fill(runs)(evaluator.evaluate(ref, res).map(metric => (ref.user, metric))).flatten
    }.toArray
    getDistributions(nodeName, metrics)
  }

  private def getDistributions(nodeName: String, metrics: Array[(String, Metric)]) =
    metrics
        .groupBy(_._2.name)
        .map { case (name, values) =>
          DistributionArtifact(s"$nodeName/$name", values.map { case (k, v) => (k, v.value) })
        }.toSeq
}