/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

package fr.cnrs.liris.accio.tools.cli.commands

import com.twitter.util.Future
import fr.cnrs.liris.accio.agent.CreateRunRequest
import fr.cnrs.liris.accio.api.Values
import fr.cnrs.liris.accio.api.thrift.{Experiment, Package, Value}
import fr.cnrs.liris.accio.dsl.ExperimentParser
import fr.cnrs.liris.accio.tools.cli.event.{Event, EventKind, Reporter}
import fr.cnrs.liris.common.util.{FileUtils, Seqs, StringUtils}

final class SubmitCommand extends Command with ClientCommand {
  private[this] val nameFlag = flag[String]("name", "Run name")
  private[this] val tagsFlag = flag[String]("tags", "Run tags (comma-separated)")
  private[this] val notesFlag = flag[String]("notes", "Run notes")
  private[this] val repeatFlag = flag[Int]("repeat", "Number of times to repeat each run")
  private[this] val seedFlag = flag[Long]("seed", "Seed to use for unstable operators")

  override def name = "submit"

  override def help = "Launch an Accio workflow."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: CommandEnvironment): Future[ExitCode] = {
    if (residue.isEmpty) {
      env.reporter.handle(Event.error("You must provide a run file or package specification."))
      return Future.value(ExitCode.CommandLineError)
    }
    val params = try {
      parseParams(residue.tail)
    } catch {
      case e: IllegalArgumentException =>
        env.reporter.handle(Event.error(s"Params argument parse error: ${e.getMessage}"))
        return Future.value(ExitCode.CommandLineError)
    }
    parseAndSubmit(residue.head, params, env.reporter)
  }

  private def parseAndSubmit(uri: String, params: Map[String, Seq[Value]], reporter: Reporter): Future[ExitCode] = {
    val file = FileUtils.expandPath(uri).toFile
    val future = if (file.exists) {
      if (!file.canRead) {
        reporter.handle(Event.error(s"Cannot read workflow definition file: ${file.getAbsolutePath}"))
        return Future.value(ExitCode.DefinitionError)
      }
      val parser = new ExperimentParser
      parser.parse(file)
    } else {
      Future.value(Experiment(pkg = parsePackage(uri)))
    }
    future
      .map(experiment => merge(experiment.copy(params = experiment.params ++ params)))
      .flatMap { experiment =>
        client
          .createRun(CreateRunRequest(experiment))
          .map { resp =>
            resp.warnings.foreach { violation =>
              reporter.handle(Event(EventKind.Warning, s"${violation.message} (at ${violation.field})"))
            }
            resp.ids.foreach(id => reporter.handle(Event.info(s"Created run $id")))
            if (resp.ids.size > 1) {
              reporter.handle(Event.info(s"Created ${resp.ids.size} runs successfully"))
            }
            ExitCode.Success
          }
      }
  }

  private def parsePackage(spec: String) =
    spec.split(':').toSeq match {
      case name :: version :: Nil => Package(name, Some(version))
      case _ => Package(spec)
    }

  private[this] val ParamRegex = "([^=]+)=(.+)".r

  private def parseParams(params: Seq[String]): Map[String, Seq[Value]] = {
    Seqs.index(params.map {
      case ParamRegex(paramName, value) => paramName -> Values.encodeString(value)
      case str => throw new IllegalArgumentException(s"Invalid param (expected key=value): $str")
    })
  }

  private def merge(spec: Experiment) = {
    var newSpec = spec
    nameFlag.get.foreach(name => newSpec = newSpec.copy(name = Some(name)))
    notesFlag.get.foreach(notes => newSpec = newSpec.copy(notes = Some(notes)))
    newSpec = newSpec.copy(tags = newSpec.tags ++ StringUtils.explode(tagsFlag.get, ","))
    repeatFlag.get.foreach(repeat => newSpec = newSpec.copy(repeat = Some(repeat)))
    seedFlag.get.foreach(seed => newSpec = newSpec.copy(seed = Some(seed)))
    newSpec
  }
}