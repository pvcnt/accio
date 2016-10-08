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
import java.util.concurrent.atomic.AtomicInteger

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.framework.OpRegistry
import fr.cnrs.liris.accio.core.param.ParamMap
import fr.cnrs.liris.accio.core.pipeline._
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.FileUtils

case class RunCommandOpts(
  @Flag(name = "workdir", help = "Working directory where to write reports and artifacts")
  workDir: Option[String],
  @Flag(name = "name", help = "Experiment name override")
  name: Option[String],
  @Flag(name = "tags", help = "Space-separated experiment tags override")
  tags: Option[String],
  @Flag(name = "notes", help = "Experiment notes override")
  notes: Option[String],
  @Flag(name = "user", help = "User who launched the experiment")
  user: Option[String],
  @Flag(name = "runs", help = "Experiment runs override")
  runs: Int = 1,
  @Flag(name = "params", help = "Experiment parameters override")
  params: Option[String])

@Command(
  name = "run",
  flags = Array(classOf[RunCommandOpts]),
  help = "Execute an Accio workflow",
  allowResidue = true)
class RunCommand @Inject()(parser: ExperimentParser, executor: ExperimentExecutor, opRegistry: OpRegistry)
  extends AccioCommand with StrictLogging {

  def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    val opts = flags.as[RunCommandOpts]
    if (flags.residue.isEmpty) {
      out.writeln("<error>Specify one or multiple files to run as arguments</error>")
      ExitCode.CommandLineError
    } else {
      val workDir = opts.workDir match {
        case Some(dir) =>
          val path = Paths.get(dir)
          require(path.toFile.isDirectory && path.toFile.canWrite, s"Invalid or unwritable directory ${path.toAbsolutePath}")
          path
        case None => Files.createTempDirectory("accio-")
      }
      out.writeln(s"Writing progress in <comment>${workDir.toAbsolutePath}</comment>")
      val progressReporter = new ConsoleGraphProgressReporter(out)
      flags.residue.foreach { url =>
        make(opts, workDir, url, progressReporter)
      }
      out.writeln(s"Done. Reports in <comment>${workDir.toAbsolutePath}</comment>")
      ExitCode.Success
    }
  }

  private def make(opts: RunCommandOpts, workDir: Path, url: String, progressReporter: ExperimentProgressReporter) = {
    var experiment = parser.parse(Paths.get(FileUtils.replaceHome(url)))
    opts.name.foreach { name =>
      experiment = experiment.copy(name = name)
    }
    opts.tags.foreach { tags =>
      experiment = experiment.copy(tags = tags.split(" ").map(_.trim).toSet)
    }
    opts.notes.foreach { notes =>
      experiment = experiment.copy(notes = Some(notes))
    }
    opts.user.foreach { user =>
      experiment = experiment.copy(initiator = User.parse(user))
    }
    if (opts.runs > 1) {
      experiment = experiment.copy(workflow = experiment.workflow.setRuns(opts.runs))
    }
    opts.params.foreach { params =>
      val NameRegex = "([^/]+)/(.+)".r
      val ParamRegex = "([^=]+)=(.+)".r
      val map = params.trim.split(" ").map {
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
    val str = s"    ${nodeDef.name}: $i/${run.graph.size}"
    out.write(str)
    if (str.length < length) {
      out.write(" " * (length - str.length))
    }
    out.write("\r")
    length = str.length
  }

  override def onNodeComplete(run: Run, nodeDef: NodeDef, successful: Boolean): Unit = {}
}