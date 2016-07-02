package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.common.util.HashUtils

import scala.collection.mutable

trait ExperimentExecutor {
  def execute(workDir: Path, experiment: ExperimentRun): ExperimentRun
}

class LocalExperimentExecutor @Inject()(workflowExecutor: RunExecutor, writer: ReportWriter)
  extends ExperimentExecutor with StrictLogging {
  override def execute(workDir: Path, experiment: ExperimentRun): ExperimentRun = {
    logger.trace(s"Starting execution of experiment ${experiment.id}: ${experiment.defn}")
    writer.write(workDir, experiment)
    var runningExperiment = experiment

    val strategy = getExecutionStrategy(runningExperiment.defn)
    var scheduled = mutable.Queue.empty[(GraphDef, Any)] ++ strategy.next
    while (scheduled.nonEmpty) {
      val (graphDef, meta) = scheduled.dequeue()
      val runId = HashUtils.sha1(UUID.randomUUID().toString)
      logger.trace(s"Starting execution of workflow run $runId: $graphDef")
      runningExperiment = runningExperiment.copy(children = runningExperiment.children ++ Seq(runId))
      var run = Run(runId, runningExperiment.id, graphDef, strategy.name(graphDef))
      writer.write(workDir, runningExperiment)

      val progressReporter = new ConsoleWorkflowProgressReporter(graphDef.size)
      val report = workflowExecutor.execute(workDir, run, progressReporter)
      scheduled ++= strategy.next(graphDef, meta, report)

      run = run.copy(report = report)
      writer.write(workDir, run)
      logger.trace(s"Completed execution of workflow run $runId")
    }

    runningExperiment = runningExperiment.complete(successful = true)
    writer.write(workDir, runningExperiment)
    logger.trace(s"Completed execution of experiment ${runningExperiment.id}")
    runningExperiment
  }

  private def getExecutionStrategy(experimentDef: ExperimentDef) = {
    val graphDef = experimentDef.paramMap match {
      case None => experimentDef.workflow.graph
      case Some(m) => experimentDef.workflow.graph.setParams(m)
    }
    if (experimentDef.exploration.isDefined) {
      new ExplorationStrategy(graphDef, experimentDef.exploration.get)
    } else if (experimentDef.optimization.isDefined) {
      new SimulatedAnnealingStrategy(graphDef, experimentDef.optimization.get)
    } else {
      new SingleExecutionStrategy(graphDef)
    }
  }
}

class ConsoleWorkflowProgressReporter(count: Int, width: Int = 80) extends WorkflowProgressReporter {
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