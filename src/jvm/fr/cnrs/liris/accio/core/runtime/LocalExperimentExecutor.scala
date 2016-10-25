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
import fr.cnrs.liris.common.util.{HashUtils, Seqs}

import scala.collection.mutable
import scala.util.Random

class LocalExperimentExecutor @Inject()(workflowExecutor: GraphExecutor, repository: ReportRepository, opRegistry: OpRegistry)
  extends ExperimentExecutor with LazyLogging {

  override def execute(experiment: Experiment, workDir: Path, progressReporter: ExperimentProgressReporter): ExperimentReport = {
    repository.write(workDir, experiment)
    var report = new ExperimentReport
    val rnd = new Random(experiment.seed)

    progressReporter.onStart(experiment)

    val scheduled = mutable.Queue.empty[(String, Graph)] ++ explore(experiment)
    while (scheduled.nonEmpty) {
      val (name, graph) = scheduled.dequeue()
      for (idx <- 0 until experiment.runs) {
        val runId = HashUtils.sha1(UUID.randomUUID().toString)
        logger.trace(s"Starting execution of workflow run $runId: $graph")
        report = report.addRun(runId)
        val seed = rnd.nextLong()
        val run = Run(id = runId, parent = experiment.id, graph = graph, name = Some(name), idx = idx, seed = seed)
        repository.write(workDir, experiment.copy(report = Some(report)))
        workflowExecutor.execute(run, workDir, progressReporter)
        logger.trace(s"Completed execution of workflow run $runId")
      }
    }

    report = report.complete()
    progressReporter.onComplete(experiment)
    repository.write(workDir, experiment.copy(report = Some(report)))
    logger.trace(s"Completed execution of experiment ${experiment.id}")

    report
  }

  private def explore(experiment: Experiment): Seq[(String, Graph)] = {
    val graph = experiment.workflow.graph
    if (experiment.params.nonEmpty) {
      val allValues = experiment.params.map { case (ref, explo) =>
        val argDef = opRegistry(graph(ref.node).op).defn.inputs.find(_.name == ref.port).get
        Values.expand(explo, argDef.kind).map(v => ref -> v)
      }.toSeq
      Seqs.crossProduct(allValues).map { params =>
        val name = params.map { case (k, v) => s"$k=$v" }.mkString(" ")
        (name, graph.setParams(params.toMap))
      }
    } else {
      Seq((experiment.name, graph))
    }
  }
}