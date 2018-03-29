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

import java.nio.file.{Files, Path}

import fr.cnrs.liris.accio.agent.{ParseRunRequest, ParseWorkflowRequest}
import fr.cnrs.liris.accio.tools.cli.event.{Event, EventKind, Reporter}
import fr.cnrs.liris.common.util.FileUtils

import scala.collection.JavaConverters._

final class ValidateCommand extends Command with ClientCommand {
  override def name = "validate"

  override def help = "Validate the syntax and semantics of Accio definition files."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: CommandEnvironment): ExitCode = {
    if (residue.isEmpty) {
      env.reporter.handle(Event.error("You must specify at least one file to validate as argument"))
      return ExitCode.CommandLineError
    }
    val outcomes = residue.map(uri => validate(uri, env.reporter))
    ExitCode.select(outcomes)
  }

  private def validate(uri: String, reporter: Reporter): ExitCode = {
    val path = FileUtils.expandPath(uri)
    if (!path.toFile.exists || !path.toFile.canRead) {
      reporter.handle(Event.error(s"Cannot read file ${path.toAbsolutePath}"))
      ExitCode.DefinitionError
    } else {
      val content = Files.readAllLines(path).asScala.mkString
      // Dirty detection of whether this file is a workflow or a run definition, based on JSON content.
      if (content.contains("\"workflow\"")) {
        validateRun(content, path, reporter)
      } else {
        validateWorkflow(content, path, reporter)
      }
    }
  }

  private def validateRun(content: String, path: Path, reporter: Reporter): ExitCode = {
    val req = ParseRunRequest(content, Map.empty, Some(path.getFileName.toString))
    respond(client.parseRun(req), reporter) { resp =>
      printErrors(resp.warnings, reporter, EventKind.Warning)
      printErrors(resp.errors, reporter, EventKind.Error)
      if (resp.run.isDefined) {
        reporter.handle(Event.info(s"Validated file ${path.toAbsolutePath}"))
        ExitCode.Success
      } else {
        ExitCode.DefinitionError
      }
    }
  }

  private def validateWorkflow(content: String, path: Path, reporter: Reporter): ExitCode = {
    val req = ParseWorkflowRequest(content, Some(path.getFileName.toString))
    respond(client.parseWorkflow(req), reporter) { resp =>
      printErrors(resp.warnings, reporter, EventKind.Warning)
      printErrors(resp.errors, reporter, EventKind.Error)
      if (resp.workflow.isDefined) {
        reporter.handle(Event.info(s"Validated file ${path.toAbsolutePath}"))
        ExitCode.Success
      } else {
        ExitCode.DefinitionError
      }
    }
  }
}