package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.dataset.{DataFrame, DatasetEnv}
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.accio.core.io.{CsvSink, CsvSource}
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.common.util.Requirements._

import scala.collection.mutable
import scala.util.control.NonFatal

trait GraphProgressReporter {
  def onGraphStart(run: Run): Unit

  def onGraphComplete(run: Run, successful: Boolean): Unit

  def onNodeStart(run: Run, nodeDef: NodeDef): Unit

  def onNodeComplete(run: Run, nodeDef: NodeDef, successful: Boolean): Unit
}

object NoGraphProgressReporter extends GraphProgressReporter {
  override def onGraphStart(run: Run): Unit = {}

  override def onNodeComplete(run: Run, nodeDef: NodeDef, successful: Boolean): Unit = {}

  override def onGraphComplete(run: Run, successful: Boolean): Unit = {}

  override def onNodeStart(run: Run, nodeDef: NodeDef): Unit = {}
}

/**
 * A graph executor is responsible for the execution of a graph specified by a [[Run]]. The execution of a single graph
 * is done locally, on a single machine.
 *
 * @param env          Dataset environment
 * @param graphBuilder Graph builder
 * @param writer       Report writer
 */
class GraphExecutor @Inject()(env: DatasetEnv, graphBuilder: GraphBuilder, writer: ReportWriter) extends LazyLogging {
  /**
   * Execute a given run. Artifacts and reports will be written inside a working directory, and a progress reporter
   * will receive updates about the graph execution progress.
   *
   * @param run              Run to execute
   * @param workDir          Working directory where to write artifacts and reports
   * @param progressReporter Reporter receiving progress updates
   * @return Final report
   */
  def execute(run: Run, workDir: Path, progressReporter: GraphProgressReporter = NoGraphProgressReporter): RunReport = {
    val graph = graphBuilder.build(run.graph)

    val outputs = mutable.Map.empty[String, Artifact]
    val scheduled = mutable.Queue.empty[Node] ++ graph.roots // Initially schedule graph roots
    var lastError: Option[Throwable] = None
    var report = new RunReport

    try {
      progressReporter.onGraphStart(run)
    } catch {
      case NonFatal(e) => logger.error("Error while reporting progress", e)
    }

    while (scheduled.nonEmpty) {
      val node = scheduled.dequeue()
      report = report.startNode(node.name)

      // We write the report and report status before executing the node.
      writeReport(workDir, run, report)
      try {
        progressReporter.onNodeStart(run, node.defn)
      } catch {
        case NonFatal(e) => logger.error("Error while reporting progress", e)
      }

      try {
        val artifacts = execute(graph, node, outputs.toMap, run.id, workDir)
        report = report.completeNode(node.name, artifacts)
        outputs ++= artifacts.map(art => art.name -> art)
      } catch {
        case NonFatal(e) =>
          report = report.completeNode(node.name, e)
          lastError = Some(e)
      }

      // We write the report and report status after executing the node.
      writeReport(workDir, run, report)
      try {
        progressReporter.onNodeComplete(run, node.defn, successful = lastError.isEmpty)
      } catch {
        case NonFatal(e) => logger.error("Error while reporting progress", e)
      }

      // Then we can schedule next steps to take.
      if (lastError.isEmpty) {
        // If the node was successfully executed, we schedule its successors for which all dependencies are available.
        scheduled ++= node.successors.map(graph(_)).filter(_.dependencies.forall(outputs.contains))
      } else {
        // Otherwise we clear the remaining scheduled nodes to exit the loop.
        scheduled.clear()
      }
    }
    report = report.complete()

    // We write the report and report status. Moreover, we log the possible error, to be sure it will not be missed.
    writeReport(workDir, run, report)
    try {
      progressReporter.onGraphComplete(run, successful = lastError.isEmpty)
    } catch {
      case NonFatal(e) => logger.error("Error while reporting progress", e)
    }
    lastError.foreach(e => logger.error(s"Error while executing run ${run.id}", e))

    // Last verification that all nodes had a chance to run.
    if (report.successful && report.nodeStats.size != run.graph.size) {
      val missing = run.graph.nodes.map(_.name).toSet.diff(report.nodeStats.map(_.name))
      throw new IllegalStateException(s"Some nodes were never executed: ${missing.mkString(", ")}")
    }

    report
  }

  private def writeReport(workDir: Path, run: Run, report: RunReport) =
    try {
      writer.write(workDir, run.copy(report = Some(report)))
    } catch {
      case NonFatal(e) => logger.error(s"Unable to write report for run ${run.id}", e)
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

  private def execute(node: Node, inputs: Seq[DataFrame[Trace]], runId: String, workDir: Path): Seq[Artifact] = {
    node.operator match {
      case s: Source[_] => execute(s, node.name, inputs, runId, workDir)
      case t: Transformer => execute(t, node.name, inputs, runId, workDir)
      case a: Analyzer[_, _] => execute(a, node.name, inputs)
      case e: Evaluator[_, _] => execute(e, node.name, inputs)
    }
  }

  private def execute(source: Source[_], nodeName: String, inputs: Seq[DataFrame[Trace]], runId: String, workDir: Path) = {
    requireState(inputs.isEmpty, s"Source requires no input (got ${inputs.size})")
    Seq(DatasetArtifact(nodeName, source.get(env)))
  }

  private def execute(transformer: Transformer, nodeName: String, inputs: Seq[DataFrame[Trace]], runId: String, workDir: Path) = {
    requireState(inputs.size == 1, s"Transformer requires exactly one input (got ${inputs.size})")
    val data = inputs.head.flatMap(transformer.transform)
    val url = workDir.resolve("data").resolve(s"$runId-$nodeName").toAbsolutePath.toString
    data.write(CsvSink(url))
    Seq(StoredDatasetArtifact(nodeName, url))
  }

  private def execute(analyzer: Analyzer[_, _], nodeName: String, inputs: Seq[DataFrame[Trace]]) = {
    requireState(inputs.size == 1, s"Analyzer requires exactly one input (got ${inputs.size})")
    val metrics = inputs.head.flatMap(trace => analyzer.analyze(trace).map(metric => (trace.id, metric))).toArray
    getDistributions(nodeName, metrics)
  }

  private def execute(evaluator: Evaluator[_, _], nodeName: String, inputs: Seq[DataFrame[Trace]]) = {
    requireState(inputs.size == 2, s"Evaluator requires exactly two inputs (got ${inputs.size})")
    val metrics = inputs.head.zip(inputs.last).flatMap { case (ref, res) =>
      requireState(ref.id == res.id, s"Trace mismatch at $nodeName: ${ref.id} / ${res.id}")
      evaluator.evaluate(ref, res).map(metric => (ref.id, metric))
    }.toArray
    getDistributions(nodeName, metrics)
  }

  private def getDistributions(nodeName: String, metrics: Array[(String, Metric)]) =
    metrics
      .groupBy(_._2.name)
      .map { case (name, values) =>
        DistributionArtifact(s"$nodeName/$name", values.map { case (k, v) => k -> v.value }.toMap)
      }.toSeq
}