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

import com.twitter.util.Future
import fr.cnrs.liris.accio.agent.{ValidateRunRequest, ValidateWorkflowRequest}
import fr.cnrs.liris.accio.api.thrift.FieldViolation
import fr.cnrs.liris.accio.dsl.{ExperimentParser, WorkflowParser}
import fr.cnrs.liris.accio.tools.cli.event.{Event, EventKind, Reporter}
import fr.cnrs.liris.util.FileUtils

import scala.collection.JavaConverters._

final class ValidateCommand extends Command with ClientCommand {
  override def name = "validate"

  override def help = "Validate the syntax and semantics of Accio definition files."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: CommandEnvironment): Future[ExitCode] = {
    if (residue.isEmpty) {
      env.reporter.handle(Event.error("You must specify at least one file to validate as argument"))
      return Future.value(ExitCode.CommandLineError)
    }
    val fs = residue.map(path => validate(path, env.reporter))
    Future.collect(fs).map(ExitCode.select)
  }

  private def validate(uri: String, reporter: Reporter): Future[ExitCode] = {
    val path = FileUtils.expandPath(uri)
    if (!path.toFile.exists || !path.toFile.canRead) {
      reporter.handle(Event.error(s"Cannot read file ${path.toAbsolutePath}"))
      return Future.value(ExitCode.DefinitionError)
    }
    val content = Files.readAllLines(path).asScala.mkString
    // Dirty detection of whether this file is a workflow or a run definition, based on JSON content.
    if (content.contains("\"workflow\"")) {
      validateRun(content, path, reporter)
    } else {
      validateWorkflow(content, path, reporter)
    }
  }

  private def validateRun(content: String, path: Path, reporter: Reporter): Future[ExitCode] = {
    val parser = new ExperimentParser
    val run = parser.parse(content)
    client
      .validateRun(ValidateRunRequest(run))
      .map(resp => handleResponse(resp.errors, resp.warnings, path: Path, reporter))
  }

  private def validateWorkflow(content: String, path: Path, reporter: Reporter): Future[ExitCode] = {
    val parser = new WorkflowParser
    val workflow = parser.parse(content, Some(path.toAbsolutePath.toString))
    client
      .validateWorkflow(ValidateWorkflowRequest(workflow))
      .map(resp => handleResponse(resp.errors, resp.warnings, path: Path, reporter))
  }

  private def handleResponse(errors: Seq[FieldViolation], warnings: Seq[FieldViolation], path: Path, reporter: Reporter) = {
    warnings.foreach { violation =>
      reporter.handle(Event(EventKind.Warning, s"${violation.message} (at ${violation.field})"))
    }
    errors.foreach { violation =>
      reporter.handle(Event(EventKind.Error, s"${violation.message} (at ${violation.field})"))
    }
    if (errors.isEmpty) {
      reporter.handle(Event.info(s"Validated file ${path.toAbsolutePath}"))
      ExitCode.Success
    } else {
      ExitCode.DefinitionError
    }
  }
}