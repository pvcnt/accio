package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.dataset.{Dataset, DatasetEnv}
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.accio.core.io.{CsvSink, CsvSource}
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.common.util.Requirements._

import scala.collection.mutable
import scala.util.control.NonFatal

trait WorkflowProgressReporter {
  def onStart(): Unit

  def onComplete(successful: Boolean): Unit

  def onNodeStart(name: String): Unit

  def onNodeComplete(name: String, successful: Boolean): Unit
}

object NoWorkflowProgressReporter extends WorkflowProgressReporter {
  override def onStart(): Unit = {}

  override def onComplete(successful: Boolean): Unit = {}

  override def onNodeComplete(name: String, successful: Boolean): Unit = {}

  override def onNodeStart(name: String): Unit = {}
}

class RunExecutor @Inject()(env: DatasetEnv, graphBuilder: GraphBuilder, writer: ReportWriter) extends LazyLogging {
  def execute(workDir: Path, run: Run, progressReporter: WorkflowProgressReporter = NoWorkflowProgressReporter): Report = {
    val graph = graphBuilder.build(run.graphDef)

    val outputs = mutable.Map.empty[String, Artifact]
    val scheduled = mutable.Queue.empty[Node] ++ graph.roots
    var lastError: Option[Throwable] = None
    var report = Report()

    progressReporter.onStart()

    while (scheduled.nonEmpty) {
      val node = scheduled.dequeue()
      report = report.start(node.name)
      progressReporter.onNodeStart(node.name)

      try {
        val artifacts = execute(graph, node, outputs.toMap, run.id, workDir)
        report = report.complete(node.name, artifacts)
        outputs ++= artifacts.map(art => art.name -> art)
      } catch {
        case NonFatal(e) =>
          report = report.complete(node.name, e)
          lastError = Some(e)
      }

      // We report the outcome to the observer and try to write the report after each node execution.
      progressReporter.onNodeComplete(node.name, lastError.isEmpty)
      try {
        writer.write(workDir, run.copy(report = report))
      } catch {
        case NonFatal(e) => logger.error(s"Unable to write report for run ${run.id} at ${node.name}", e)
      }

      // Then we can schedule next steps to take.
      if (lastError.isEmpty) {
        // If the node was successfully executed, we scheduled its successors for which all dependencies are available.
        scheduled ++= node.successors.map(graph(_)).filter(_.dependencies.forall(outputs.contains))
      } else {
        // Otherwise we clear the remaining scheduled nodes to exit the loop.
        scheduled.clear()
      }
    }

    progressReporter.onComplete(successful = lastError.isEmpty)

    lastError match {
      case Some(e) =>
        logger.error(s"Error while executing run ${run.id}", e)
        report.complete(e)
      case None => report.complete(successful = true)
    }
  }

  private def execute(graph: Graph, node: Node, outputs: Map[String, Artifact], runId: String, workDir: Path): Seq[Artifact] = {
    val inputs = node.dependencies.map { dep =>
      val node = graph(dep)
      outputs(node.name) match {
        case DatasetArtifact(_, data) => data
        case StoredDatasetArtifact(_, url) => env.read(CsvSource(url))
        case out => throw new RuntimeException(s"Cannot use $out as an input dependency")
      }
    }
    execute(node, inputs, runId, workDir)
  }

  private def execute(node: Node, inputs: Seq[Dataset[Trace]], runId: String, workDir: Path): Seq[Artifact] = {
    node.operator match {
      case s: Source => execute(s, node.name, node.ephemeral, inputs, runId, workDir)
      case t: Transformer => execute(t, node.name, node.ephemeral, inputs, runId, workDir)
      case a: Analyzer => execute(a, node.name, node.runs, inputs)
      case e: Evaluator => execute(e, node.name, node.runs, inputs)
    }
  }

  private def execute(source: Source, nodeName: String, ephemeral: Boolean, inputs: Seq[Dataset[Trace]], runId: String, workDir: Path) = {
    requireState(inputs.isEmpty, s"Source requires no input (got ${inputs.size})")
    Seq(getDataset(source.get(env), nodeName, ephemeral, runId, workDir))
  }

  private def execute(transformer: Transformer, nodeName: String, ephemeral: Boolean, inputs: Seq[Dataset[Trace]], runId: String, workDir: Path) = {
    requireState(inputs.size == 1, s"Transformer requires exactly one input (got ${inputs.size})")
    val data = inputs.head.flatMap(transformer.transform)
    Seq(getDataset(data, nodeName, ephemeral, runId, workDir))
  }

  private def execute(analyzer: Analyzer, nodeName: String, runs: Int, inputs: Seq[Dataset[Trace]]) = {
    requireState(inputs.size == 1, s"Analyzer requires exactly one input (got ${inputs.size})")
    val metrics = Array.fill(runs) {
      inputs.head.flatMap(trace => analyzer.analyze(trace).map(metric => (trace.user, metric))).toArray
    }.flatten
    getDistributions(nodeName, metrics)
  }

  private def execute(evaluator: Evaluator, nodeName: String, runs: Int, inputs: Seq[Dataset[Trace]]) = {
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