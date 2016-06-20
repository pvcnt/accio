package fr.cnrs.liris.accio.core.pipeline

import java.nio.file.Path
import java.util.UUID

import com.google.inject.Inject
import com.twitter.util.StorageUnit
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.framework._
import org.joda.time.Instant

import scala.collection.mutable

trait ExperimentExecutor {
  def execute(workDir: Path, id: String, experimentDef: ExperimentDef): Unit
}

class LocalExperimentExecutor @Inject()(graphBuilder: GraphBuilder, graphExecutor: GraphExecutor, writer: ExperimentWriter)
    extends ExperimentExecutor with StrictLogging {
  override def execute(workDir: Path, id: String, experimentDef: ExperimentDef): Unit = {
    logger.trace(s"Starting execution of experiment $id: $experimentDef")
    val startedAt = Instant.now
    var experiment = Experiment(id, experimentDef)
    writer.write(workDir, experiment)

    val strategy = getExecutionStrategy(experimentDef)
    var scheduled = mutable.Queue.empty[(GraphDef, Any)] ++ strategy.next
    while (scheduled.nonEmpty) {
      val (graphDef, meta) = scheduled.dequeue()
      val runId = UUID.randomUUID().toString
      logger.trace(s"Starting execution of workflow run $runId: $graphDef")
      var run = WorkflowRun(runId, None, graphDef, None)
      experiment = experiment.copy(children = experiment.children ++ Seq(runId))
      writer.write(workDir, run)
      writer.write(workDir, experiment)

      val graph = graphBuilder.build(graphDef)
      val report = graphExecutor.execute(graph)
      scheduled ++= strategy.next(graphDef, meta, report)

      run = run.copy(report = Some(report.withoutEphemeral))
      writer.write(workDir, run)
      logger.trace(s"Completed execution of workflow run $runId")
    }

    val stats = ExecStats(startedAt, Instant.now, successful = true, StorageUnit.zero)
    experiment = experiment.copy(stats = Some(stats))
    writer.write(workDir, experiment)
    logger.trace(s"Completed execution of experiment $id")
  }

  private def getExecutionStrategy(experimentDef: ExperimentDef) = {
    val graphDef = experimentDef.paramMap match {
      case None => experimentDef.workflow.graph
      case Some(m) => experimentDef.workflow.graph.set(m)
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