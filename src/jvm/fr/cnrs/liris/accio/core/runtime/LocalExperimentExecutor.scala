/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Accio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Accio.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnrs.liris.accio.core.runtime

import java.nio.file.Path
import java.util.UUID

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.common.util.HashUtils

import scala.collection.mutable

class LocalExperimentExecutor @Inject()(workflowExecutor: GraphExecutor, repository: ReportRepository)
  extends ExperimentExecutor with LazyLogging {

  override def execute(experiment: Experiment, workDir: Path, progressReporter: ExperimentProgressReporter): ExperimentReport = {
    repository.write(workDir, experiment)
    var report = new ExperimentReport

    progressReporter.onStart(experiment)

    val scheduled = mutable.Queue.empty[(String, Graph)] ++ explore(experiment)
    while (scheduled.nonEmpty) {
      val (name, graph) = scheduled.dequeue()
      val runId = HashUtils.sha1(UUID.randomUUID().toString)
      logger.trace(s"Starting execution of workflow run $runId: $graph")
      report = report.addRun(runId)
      val run = Run(runId, experiment.id, graph, Some(name))
      repository.write(workDir, experiment.copy(report = Some(report)))

      workflowExecutor.execute(run, workDir, progressReporter)

      logger.trace(s"Completed execution of workflow run $runId")
    }

    report = report.complete()
    progressReporter.onComplete(experiment)
    repository.write(workDir, experiment.copy(report = Some(report)))
    logger.trace(s"Completed execution of experiment ${experiment.id}")

    report
  }

  private def explore(experiment: Experiment): Seq[(String, Graph)] = {
    val graph = experiment.workflow.graph.setParams(experiment.params)
    Seq((experiment.name, graph))
    /*if (experiment.exploration.isDefined) {
      experiment.exploration.get.paramGrid.toSeq.map { params =>
        (params.toString, graphDef.setParams(params))
      }
    } else {
      Seq((experiment.name, graph))
    }*/
  }
}