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

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.framework._

class LocalExperimentExecutor @Inject()(graphExecutor: GraphExecutor, repository: ReportRepository, runFactory: RunFactory)
  extends ExperimentExecutor with LazyLogging {

  override def execute(experiment: Experiment, workDir: Path, progressReporter: ExperimentProgressReporter): ExperimentReport = {
    repository.write(workDir, experiment)
    var report = new ExperimentReport

    progressReporter.onStart(experiment)

    val runs = runFactory.create(experiment)
    runs.foreach { run =>
      logger.trace(s"Starting execution of workflow run ${run.id}")
      report = report.addRun(run.id)
      repository.write(workDir, experiment.copy(report = Some(report)))
      graphExecutor.execute(run, experiment.workflow, workDir, progressReporter)
      logger.trace(s"Completed execution of workflow run ${run.id}")
    }

    report = report.complete()
    progressReporter.onComplete(experiment)
    repository.write(workDir, experiment.copy(report = Some(report)))
    logger.trace(s"Completed execution of experiment ${experiment.id}")

    report
  }
}