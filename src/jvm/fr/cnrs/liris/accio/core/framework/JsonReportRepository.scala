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

package fr.cnrs.liris.accio.core.framework

import java.io.{FileInputStream, FileOutputStream}
import java.nio.file.Path

import com.google.inject.{Inject, Singleton}
import com.twitter.finatra.json.FinatraObjectMapper

@Singleton
class JsonReportRepository @Inject()(mapper: FinatraObjectMapper) extends ReportRepository {
  override def readExperiment(workDir: Path, id: String): Experiment = {
    mapper.parse[Experiment](new FileInputStream(workDir.resolve(s"experiment-$id.json").toFile))
  }

  override def readRun(workDir: Path, id: String): Run = {
    mapper.parse[Run](new FileInputStream(workDir.resolve(s"run-$id.json").toFile))
  }

  override def write(workDir: Path, experiment: Experiment): Unit = {
    val os = new FileOutputStream(workDir.resolve(s"experiment-${experiment.id}.json").toFile)
    try {
      mapper.writeValue(experiment, os)
    } finally {
      os.close()
    }
  }

  override def write(workDir: Path, run: Run): Unit = {
    val os = new FileOutputStream(workDir.resolve(s"run-${run.id}.json").toFile)
    try {
      mapper.writeValue(run, os)
    } finally {
      os.close()
    }
  }
}