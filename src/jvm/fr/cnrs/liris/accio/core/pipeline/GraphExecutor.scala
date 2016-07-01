package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.dataset.{Dataset, DatasetEnv}
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.accio.core.io.{CsvSink, CsvSource}
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.common.util.Requirements._
import org.joda.time.Instant

import scala.collection.mutable

class GraphExecutor @Inject()(env: DatasetEnv, writer: ReportWriter) extends StrictLogging {
  def execute(graph: Graph, runId: String, workDir: Path): Report = {
    val startedAt = Instant.now()
    val outputs = mutable.Map.empty[String, Artifact]
    val nodeStats = mutable.Map.empty[String, ExecStats]
    val scheduled = mutable.Queue.empty[Node] ++ graph.roots
    while (scheduled.nonEmpty) {
      val node = scheduled.dequeue()
      logger.trace(s"Starting execution of node ${node.name}")
      val startedAt = Instant.now()
      val artifacts = run(graph, node, outputs.toMap, runId, workDir)
      outputs ++= artifacts.map(art => art.name -> art)
      nodeStats(node.name) = ExecStats(startedAt, Some(Instant.now()), successful = Some(true))
      scheduled ++= node.successors.map(graph(_)).filter(_.dependencies.forall(outputs.contains))
      logger.trace(s"Completed execution of node ${node.name}")
    }
    val stats = ExecStats(startedAt, Some(Instant.now()), successful = Some(true))
    Report(stats, nodeStats.toMap, outputs.values.toSeq)
  }

  private def run(graph: Graph, node: Node, outputs: Map[String, Artifact], runId: String, workDir: Path): Seq[Artifact] = {
    val inputs = node.dependencies.map { dep =>
      val node = graph(dep)
      outputs(node.name) match {
        case DatasetArtifact(_, data) => data
        case StoredDatasetArtifact(_, url) => env.read(CsvSource(url))
        case out => throw new RuntimeException(s"Cannot use $out as an input dependency")
      }
    }
    run(node, inputs, runId, workDir)
  }

  private def run(node: Node, inputs: Seq[Dataset[Trace]], runId: String, workDir: Path): Seq[Artifact] = {
    node.operator match {
      case s: Source => run(s, node.name, node.ephemeral, inputs, runId, workDir)
      case t: Transformer => run(t, node.name, node.ephemeral, inputs, runId, workDir)
      case a: Analyzer => run(a, node.name, node.runs, inputs)
      case e: Evaluator => run(e, node.name, node.runs, inputs)
    }
  }

  private def run(source: Source, nodeName: String, ephemeral: Boolean, inputs: Seq[Dataset[Trace]], runId: String, workDir: Path) = {
    requireState(inputs.isEmpty, s"Source requires no input (got ${inputs.size})")
    Seq(getDataset(source.get(env), nodeName, ephemeral, runId, workDir))
  }

  private def run(transformer: Transformer, nodeName: String, ephemeral: Boolean, inputs: Seq[Dataset[Trace]], runId: String, workDir: Path) = {
    requireState(inputs.size == 1, s"Transformer requires exactly one input (got ${inputs.size})")
    val data = inputs.head.flatMap(transformer.transform)
    Seq(getDataset(data, nodeName, ephemeral, runId, workDir))
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

  private def getDataset(data: Dataset[Trace], nodeName: String, ephemeral: Boolean, runId: String, workDir: Path) = {
    if (ephemeral) {
      DatasetArtifact(nodeName, data)
    } else {
      val url = workDir.resolve("data").resolve(s"$runId-$nodeName").toAbsolutePath.toString
      data.write(CsvSink(url))
      StoredDatasetArtifact(nodeName, url)
    }
  }

  private def getDistributions(nodeName: String, metrics: Array[(String, Metric)]) =
    metrics
      .groupBy(_._2.name)
      .map { case (name, values) =>
        DistributionArtifact(s"$nodeName/$name", values.map { case (k, v) => (k, v.value) })
      }.toSeq
}