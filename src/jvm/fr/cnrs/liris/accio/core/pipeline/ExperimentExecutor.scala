package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path
import java.util.UUID

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.common.util.HashUtils

import scala.collection.mutable

trait ExperimentProgressReporter extends GraphProgressReporter {
  def onStart(experiment: Experiment): Unit

  def onComplete(experiment: Experiment): Unit
}

object NoExperimentProgressReporter extends ExperimentProgressReporter {
  override def onStart(experiment: Experiment): Unit = {}

  override def onComplete(experiment: Experiment): Unit = {}

  override def onNodeComplete(run: Run, nodeDef: NodeDef, successful: Boolean): Unit = {}

  override def onGraphComplete(run: Run, successful: Boolean): Unit = {}

  override def onGraphStart(run: Run): Unit = {}

  override def onNodeStart(run: Run, nodeDef: NodeDef): Unit = {}
}

trait ExperimentExecutor {
  def execute(experiment: Experiment, workDir: Path, progressReporter: ExperimentProgressReporter = NoExperimentProgressReporter): ExperimentReport
}

class LocalExperimentExecutor @Inject()(workflowExecutor: GraphExecutor, writer: ReportWriter)
  extends ExperimentExecutor with LazyLogging {
  override def execute(experiment: Experiment, workDir: Path, progressReporter: ExperimentProgressReporter): ExperimentReport = {
    writer.write(workDir, experiment)
    var report = new ExperimentReport

    progressReporter.onStart(experiment)

    val scheduled = mutable.Queue.empty[(String, GraphDef)] ++ explore(experiment)
    while (scheduled.nonEmpty) {
      val (name, graphDef) = scheduled.dequeue()
      val runId = HashUtils.sha1(UUID.randomUUID().toString)
      logger.trace(s"Starting execution of workflow run $runId: $graphDef")
      report = report.addRun(runId)
      val run = Run(runId, experiment.id, graphDef, Some(name))
      writer.write(workDir, experiment.copy(report = Some(report)))

      workflowExecutor.execute(run, workDir, progressReporter)

      logger.trace(s"Completed execution of workflow run $runId")
    }

    report = report.complete()
    progressReporter.onComplete(experiment)
    writer.write(workDir, experiment.copy(report = Some(report)))
    logger.trace(s"Completed execution of experiment ${experiment.id}")

    report
  }

  private def explore(experiment: Experiment): Seq[(String, GraphDef)] = {
    val graphDef = experiment.paramMap match {
      case None => experiment.workflow.graph
      case Some(m) => experiment.workflow.graph.setParams(m)
    }
    if (experiment.exploration.isDefined) {
      experiment.exploration.get.paramGrid.toSeq.map { params =>
        (params.toString, graphDef.setParams(params))
      }
    } else {
      Seq((experiment.name, graphDef))
    }
  }
}