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

package fr.cnrs.liris.accio.cli.commands

import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.atomic.AtomicInteger

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.cli.{Command, Reporter}
import fr.cnrs.liris.accio.core.framework.OpRegistry
import fr.cnrs.liris.accio.core.param.ParamMap
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
    @Flag(name = "user")
    user: String = "",
    @Flag(name = "runs")
    runs: Int = 1,
    @Flag(name = "params")
    params: String = "")

@Command(
  name = "run",
  help = "Execute an Accio workflow",
  allowResidue = true)
class RunCommand @Inject()(parser: ExperimentParser, executor: ExperimentExecutor, opRegistry: OpRegistry)
    extends AccioCommand[MakeCommandOpts] with StrictLogging {

  def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    val opts = flags.as[MakeCommandOpts]
    if (flags.residue.isEmpty) {
      out.writeln("<error>Specify one or multiple files to run as arguments</error>")
      ExitCode.CommandLineError
    } else {
      val workDir = if (opts.workDir.nonEmpty) Paths.get(opts.workDir) else Files.createTempDirectory("accio-")
      out.writeln(s"Writing progress in <comment>${workDir.toAbsolutePath}</comment>")
      val progressReporter = new ConsoleGraphProgressReporter(out)
      flags.residue.foreach { url =>
        make(opts, workDir, url, progressReporter)
      }
      out.writeln(s"Done. Reports in <comment>${workDir.toAbsolutePath}</comment>")
      ExitCode.Success
    }
  }

  private def make(opts: MakeCommandOpts, workDir: Path, url: String, progressReporter: ExperimentProgressReporter) = {
    var experiment = parser.parse(Paths.get(FileUtils.replaceHome(url)))
    if (opts.name.nonEmpty) {
      experiment = experiment.copy(name = opts.name)
    }
    if (opts.tags.nonEmpty) {
      experiment = experiment.copy(tags = opts.tags.split(" ").map(_.trim).toSet)
    }
    if (opts.notes.nonEmpty) {
      experiment = experiment.copy(notes = Some(opts.notes))
    }
    if (opts.user.nonEmpty) {
      experiment = experiment.copy(initiator = User.parse(opts.user))
    }
    if (opts.runs > 1) {
      experiment = experiment.copy(workflow = experiment.workflow.setRuns(opts.runs))
    }
    if (opts.params.nonEmpty) {
      val NameRegex = "([^/]+)/(.+)".r
      val ParamRegex = "([^=]+)=(.+)".r
      val map = opts.params.trim.split(" ").map {
        case ParamRegex(name, value) => name match {
          case NameRegex(nodeName, paramName) =>
            val maybeNode = experiment.workflow.graph.nodes.find(_.name == nodeName)
            require(maybeNode.isDefined, s"Unknown node: $nodeName")
            val maybeParamDef = opRegistry(maybeNode.get.op).defn.params.find(_.name == paramName)
            require(maybeParamDef.isDefined, s"Unknown param: ${maybeNode.get.op}/$paramName")
            name -> Params.parse(maybeParamDef.get.typ, value)
          case _ => throw new IllegalArgumentException(s"Invalid param name: $name")
        }
        case str => throw new IllegalArgumentException(s"Invalid param (expected key=value): $str")
      }.toMap
      experiment = experiment.setParams(new ParamMap(map))
    }
    executor.execute(experiment, workDir, progressReporter)
  }
}

private class ConsoleGraphProgressReporter(out: Reporter, width: Int = 80) extends ExperimentProgressReporter {
  private[this] val progress = new AtomicInteger
  private[this] var length = 0

  override def onStart(experiment: Experiment): Unit = synchronized {
    out.writeln(s"Experiment ${experiment.shortId}: <info>${experiment.name}</info>")
  }

  override def onComplete(experiment: Experiment): Unit = {}

  override def onGraphStart(run: Run): Unit = synchronized {
    out.writeln(s"  Run ${run.shortId}: <info>${run.name.getOrElse("(anonymous)")}</info>")
  }

  override def onGraphComplete(run: Run, successful: Boolean): Unit = synchronized {
    out.write(s"    ${" " * length}\r")
    length = 0
  }

  override def onNodeStart(run: Run, nodeDef: NodeDef): Unit = synchronized {
    val i = progress.incrementAndGet
    val str = s"    ${nodeDef.name}: $i/${run.graphDef.size}"
    out.write(str)
    if (str.length < length) {
      out.write(" " * (length - str.length))
    }
    out.write("\r")
    length = str.length
  }

  override def onNodeComplete(run: Run, nodeDef: NodeDef, successful: Boolean): Unit = {}
}