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

import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.atomic.AtomicInteger

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.accio.core.runtime.{ExperimentExecutor, ExperimentProgressReporter}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.StringUtils

case class RunOptions(
  @Flag(name = "workdir", help = "Working directory where to write reports and artifacts")
  workDir: Option[String],
  @Flag(name = "name", help = "Experiment name")
  name: Option[String],
  @Flag(name = "tags", help = "Space-separated experiment tags")
  tags: Option[String],
  @Flag(name = "notes", help = "Experiment notes")
  notes: Option[String],
  @Flag(name = "repeat", help = "Number of times to repeat each run")
  repeat: Option[Int],
  @Flag(name = "seed", help = "Seed to use for unstable operators")
  seed: Option[Long],
  @Flag(name = "user", help = "User who launched the experiment")
  user: Option[String],
  @Flag(name = "params", help = "Experiment parameters")
  params: Option[String])

@Cmd(
  name = "run",
  flags = Array(classOf[RunOptions]),
  help = "Execute an Accio workflow.",
  allowResidue = true)
class RunCommand @Inject()(experimentFactory: ExperimentFactory, executor: ExperimentExecutor, opRegistry: OpRegistry)
  extends Command with StrictLogging {

  def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    val opts = flags.as[RunOptions]
    if (flags.residue.isEmpty) {
      out.writeln("<error>Specify one or multiple files to run as arguments</error>")
      ExitCode.CommandLineError
    } else {
      val startedAt = System.currentTimeMillis()
      val workDir = getWorkDir(opts)
      out.writeln(s"Writing progress in <comment>${workDir.toAbsolutePath}</comment>")
      val progressReporter = new ConsoleGraphProgressReporter(out)
      flags.residue.foreach { url =>
        make(opts, workDir, url, progressReporter)
      }

      val duration = System.currentTimeMillis() - startedAt
      out.writeln(s"Done in ${duration / 1000}s. Reports in <comment>${workDir.toAbsolutePath}</comment>")
      ExitCode.Success
    }
  }

  private def make(opts: RunOptions, workDir: Path, experimentUri: String, progressReporter: ExperimentProgressReporter) = {
    val expArgs = ExperimentArgs(
      owner = opts.user.map(User.parse),
      name = opts.name,
      notes = opts.notes,
      tags = StringUtils.explode(opts.tags, ","),
      repeat = opts.repeat,
      seed = opts.seed,
      params = opts.params.map(parseParams).getOrElse(Map.empty))
    val experiment = experimentFactory.create(experimentUri, expArgs)
    executor.execute(experiment, workDir, progressReporter)
  }

  private def parseParams(params: String): Map[String, String] = {
    val ParamRegex = "([^=]+)=(.+)".r
    params.trim.split(" ").map {
      case ParamRegex(paramName, value) => paramName -> value
      case str => throw new IllegalArgumentException(s"Invalid param (expected key=value): $str")
    }.toMap
  }

  private def getWorkDir(opts: RunOptions) =
    opts.workDir match {
      case Some(dir) =>
        val path = Paths.get(dir)
        require(path.toFile.isDirectory, s"Invalid directory ${path.toAbsolutePath}")
        require(path.toFile.canWrite, s"Cannot write to ${path.toAbsolutePath}")
        path
      case None => Files.createTempDirectory("accio-")
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
    out.writeln(s"  Run ${run.shortId}: <info>${run.name}</info>")
  }

  override def onGraphComplete(run: Run, successful: Boolean): Unit = synchronized {
    out.write(s"    ${" " * length}\r")
    length = 0
    progress.set(0)
  }

  override def onNodeStart(run: Run, node: Node): Unit = synchronized {
    val i = progress.incrementAndGet
    //val str = s"    ${node.name}: $i/${run.graph.size}"
    val str = s"    ${node.name}"
    out.write(str)
    if (str.length < length) {
      out.write(" " * (length - str.length))
    }
    out.write("\r")
    length = str.length
  }

  override def onNodeComplete(run: Run, node: Node, successful: Boolean): Unit = {}
}