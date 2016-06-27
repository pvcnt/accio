package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path
import java.util.UUID

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.common.util.HashUtils
import org.joda.time.Instant

import scala.collection.mutable

trait ExperimentExecutor {
  def execute(workDir: Path, experiment: ExperimentRun): ExperimentRun
}

class LocalExperimentExecutor @Inject()(graphBuilder: GraphBuilder, graphExecutor: GraphExecutor, writer: ReportWriter)
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
      var run = WorkflowRun(runId, runningExperiment.id, graphDef, strategy.name(graphDef))
      writer.write(workDir, runningExperiment)

      val graph = graphBuilder.build(graphDef)
      val report = graphExecutor.execute(graph)
      scheduled ++= strategy.next(graphDef, meta, report)

      run = run.copy(report = report.complete(successful = true))
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