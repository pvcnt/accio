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

import java.nio.file.Files

import fr.cnrs.liris.accio.agent.{CreateRunRequest, ParseRunRequest}
import fr.cnrs.liris.accio.api.Utils
import fr.cnrs.liris.accio.api.thrift.RunSpec
import fr.cnrs.liris.accio.tools.cli.event.{Event, EventKind, Reporter}
import fr.cnrs.liris.common.util.{FileUtils, StringUtils}

import scala.collection.JavaConverters._

final class SubmitCommand extends Command with ClientCommand {
  private[this] val nameFlag = flag[String]("name", "Run name")
  private[this] val tagsFlag = flag[String]("tags", "Run tags (comma-separated)")
  private[this] val notesFlag = flag[String]("notes", "Run notes")
  private[this] val repeatFlag = flag[Int]("repeat", "Number of times to repeat each run")
  private[this] val seedFlag = flag[Long]("seed", "Seed to use for unstable operators")

  override def name = "submit"

  override def help = "Launch an Accio workflow."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: CommandEnvironment): ExitCode = {
    if (residue.isEmpty) {
      env.reporter.handle(Event.error("You must provide a run file or package specification."))
      ExitCode.CommandLineError
    } else {
      val params = try {
        parseParams(residue.tail)
      } catch {
        case e: IllegalArgumentException =>
          env.reporter.handle(Event.error(s"Params argument parse error: ${e.getMessage}"))
          return ExitCode.CommandLineError
      }
      parseAndSubmit(residue.head, params, env.reporter)
    }
  }

  private def parseAndSubmit(uri: String, params: Map[String, String], reporter: Reporter): ExitCode = {
    val path = FileUtils.expandPath(uri)
    val file = path.toFile
    val content = if (file.exists) {
      if (!file.canRead) {
        reporter.handle(Event.error(s"Cannot read workflow definition file: ${path.toAbsolutePath}"))
        return ExitCode.DefinitionError
      }
      Files.readAllLines(path).asScala.mkString
    } else {
      uri
    }

    val req = ParseRunRequest(content, params, Some(path.getFileName.toString))
    respond(client.parseRun(req), reporter) { resp =>
      printErrors(resp.warnings, reporter, EventKind.Warning)
      printErrors(resp.errors, reporter, EventKind.Error)
      resp.run match {
        case Some(spec) =>
          val mergedSpec = merge(spec)
          submit(mergedSpec, reporter)
        case None =>
          reporter.handle(Event.error("Some errors where found in the run definition"))
          ExitCode.DefinitionError
      }
    }
  }

  private def submit(spec: RunSpec, reporter: Reporter) = {
    val req = CreateRunRequest(spec, Utils.DefaultUser)
    respond(client.createRun(req), reporter) { resp =>
      resp.ids.foreach {
        runId =>
          reporter.handle(Event.info(s"Created run ${runId.value}"))
      }
      if (resp.ids.size > 1) {
        reporter.handle(Event.info(s"Created ${resp.ids.size} runs successfully"))
      }
      ExitCode.Success
    }
  }

  private[this] val ParamRegex = "([^=]+)=(.+)".r

  private def parseParams(params: Seq[String]): Map[String, String] = {
    params.map {
      case ParamRegex(paramName, value) => paramName -> value
      case str => throw new IllegalArgumentException(s"Invalid param (expected key=value): $str")
    }.toMap
  }

  private def merge(spec: RunSpec) = {
    var newSpec = spec
    nameFlag.get.foreach(name => newSpec = newSpec.copy(name = Some(name)))
    notesFlag.get.foreach(notes => newSpec = newSpec.copy(notes = Some(notes)))
    newSpec = newSpec.copy(tags = newSpec.tags ++ StringUtils.explode(tagsFlag.get, ","))
    repeatFlag.get.foreach(repeat => newSpec = newSpec.copy(repeat = Some(repeat)))
    seedFlag.get.foreach(seed => newSpec = newSpec.copy(seed = Some(seed)))
    newSpec
  }
}