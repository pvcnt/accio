package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.common.util.HashUtils

import scala.collection.mutable

trait ExperimentExecutor {
  def execute(experiment: Experiment, workDir: Path): ExperimentReport
}

class LocalExperimentExecutor @Inject()(workflowExecutor: GraphExecutor, writer: ReportWriter)
    extends ExperimentExecutor with StrictLogging {
  override def execute(experiment: Experiment, workDir: Path): ExperimentReport = {
    logger.trace(s"Starting execution of experiment ${experiment.id}")
    writer.write(workDir, experiment)
    var report = new ExperimentReport

    val strategy = getExecutionStrategy(experiment)
    var scheduled = mutable.Queue.empty[(GraphDef, Any)] ++ strategy.next
    while (scheduled.nonEmpty) {
      val (graphDef, meta) = scheduled.dequeue()
      val runId = HashUtils.sha1(UUID.randomUUID().toString)
      logger.trace(s"Starting execution of workflow run $runId: $graphDef")
      report = report.addRun(runId)
      val run = Run(runId, experiment.id, graphDef, strategy.name(graphDef))
      writer.write(workDir, experiment.copy(report = Some(report)))

      val progressReporter = new ConsoleGraphProgressReporter(graphDef.size)
      val runReport = workflowExecutor.execute(run, workDir, progressReporter)
      scheduled ++= strategy.next(graphDef, meta, runReport)

      logger.trace(s"Completed execution of workflow run $runId")
    }

    report = report.complete()
    writer.write(workDir, experiment.copy(report = Some(report)))
    logger.trace(s"Completed execution of experiment ${experiment.id}")

    report
  }

  private def getExecutionStrategy(experiment: Experiment) = {
    val graphDef = experiment.paramMap match {
      case None => experiment.workflow.graph
      case Some(m) => experiment.workflow.graph.setParams(m)
    }
    if (experiment.exploration.isDefined) {
      new ExplorationStrategy(graphDef, experiment.exploration.get)
    } else if (experiment.optimization.isDefined) {
      new SimulatedAnnealingStrategy(graphDef, experiment.optimization.get)
    } else {
      new SingleExecutionStrategy(graphDef)
    }
  }
}

class ConsoleGraphProgressReporter(count: Int, width: Int = 80) extends GraphProgressReporter {
  private[this] val progress = new AtomicInteger
  private[this] var length = 0

  override def onStart(): Unit = {}

  override def onComplete(successful: Boolean): Unit = {
    print(s"${" " * length}\r")
    length = 0
  }

  override def onNodeStart(name: String): Unit = {
    val i = progress.incrementAndGet
    val str = s"$name: $i/$count"
    print(str)
    if (str.length < length) {
      print(" " * (length - str.length))
    }
    print("\r")
    length = str.length
  }

  override def onNodeComplete(name: String, successful: Boolean): Unit = {}
}