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

package fr.cnrs.liris.accio.cli

import java.nio.file.Paths

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.framework.ReportRepository
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}

case class ExportFlags(
  @Flag(name = "sep", help = "Separator to use")
  separator: String = ",",
  @Flag(name = "artifacts", help = "Specify a comma-separated list of artifacts to take into account")
  artifacts: String = "NUMERIC")

@Cmd(
  name = "export",
  flags = Array(classOf[ExportFlags]),
  help = "Generate CSV reports.",
  allowResidue = true)
class ExportCommand @Inject()(repository: ReportRepository) extends AccioCommand {

  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    val reports = flags.residue.flatMap { path =>
      val workDir = Paths.get(path)
      require(workDir.toFile.exists, s"Directory ${workDir.toAbsolutePath} does not exist")
      workDir.toFile.list
        .filter(_.startsWith("run-"))
        .map(_.drop(4).dropRight(5))
        .flatMap(id => repository.readRun(workDir, id))
    }
    val opts = flags.as[ExportFlags]
    val reportStats = new ReportStatistics(reports)
    val artifacts = if (opts.artifacts == "ALL") {
      reportStats.artifacts
    } else if (opts.artifacts == "NUMERIC") {
      reportStats.artifacts.map { case (name, arts) => name -> arts.filter { case (k, v) => v.kind.isNumeric } }
    } else {
      val validNames = opts.artifacts.split(",").map(_.trim).toSet
      reportStats.artifacts.filter { case (name, _) => validNames.contains(name) }
    }

    artifacts.foreach { case (artifactName, arts) =>

    }

    ExitCode.Success
  }
}