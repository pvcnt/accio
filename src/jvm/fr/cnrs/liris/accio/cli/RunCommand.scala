/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.accio.cli

import java.nio.file.{Files, Path, Paths}
import java.util.UUID

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.pipeline._
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.FileUtils

case class MakeCommandOpts(
    @Flag(name = "workdir")
    workDir: String = "",
    @Flag(name = "name")
    name: String = "",
    @Flag(name = "tags")
    tags: String = "",
    @Flag(name = "notes")
    notes: String = "",
    @Flag(name = "runs")
    runs: Int = 1)

@Command(
  name = "run",
  help = "Execute an Accio workflow",
  allowResidue = true)
class RunCommand @Inject()(parser: ExperimentParser, executor: ExperimentExecutor)
    extends AccioCommand[MakeCommandOpts] with StrictLogging {

  def run(flags: FlagsProvider, out: Reporter): ExitCode = {
    val opts = flags.as[MakeCommandOpts]
    val workDir = if (opts.workDir.nonEmpty) Paths.get(opts.workDir) else Files.createTempDirectory("accio-")
    out.writeln(s"Persisting artifacts and reports to <comment>${workDir.toAbsolutePath}</comment>")
    flags.residue.foreach { url =>
      make(opts, workDir, url)
    }
    ExitCode.Success
  }

  private def make(opts: MakeCommandOpts, workDir: Path, url: String) = {
    val id = UUID.randomUUID().toString
    var experimentDef = parser.parse(Paths.get(FileUtils.replaceHome(url)))
    if (opts.name.nonEmpty) {
      experimentDef = experimentDef.copy(name = opts.name)
    }
    if (opts.tags.nonEmpty) {
      experimentDef = experimentDef.copy(tags = opts.tags.split(",").toSet)
    }
    if (opts.notes.nonEmpty) {
      experimentDef = experimentDef.copy(notes = Some(opts.notes))
    }
    if (opts.runs > 1) {
      experimentDef = experimentDef.copy(workflow = experimentDef.workflow.setRuns(opts.runs))
    }
    executor.execute(workDir, id, experimentDef)
  }
}