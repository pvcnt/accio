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

package fr.cnrs.liris.accio.cli

import java.io.File

import com.twitter.util.Future
import fr.cnrs.liris.accio.domain.thrift.ThriftAdapter
import fr.cnrs.liris.accio.dsl.json.JsonWorkflowParser
import fr.cnrs.liris.accio.server.ValidateWorkflowRequest
import fr.cnrs.liris.infra.cli.app.{Environment, ExitCode, Reporter}
import fr.cnrs.liris.infra.thriftserver.FieldViolation
import fr.cnrs.liris.util.FileUtils

final class ValidateCommand extends AccioCommand {
  override def name = "validate"

  override def help = "Validate the syntax and semantics of Accio job definition files."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: Environment): Future[ExitCode] = {
    if (residue.isEmpty) {
      env.reporter.error("You must specify at least one file to validate as argument")
      return Future.value(ExitCode.CommandLineError)
    }
    val fs = residue.map(path => validate(path, env))
    Future.collect(fs).map(ExitCode.select)
  }

  private def validate(uri: String, env: Environment): Future[ExitCode] = {
    val file = FileUtils.expandPath(uri).toFile
    if (!file.canRead) {
      env.reporter.error(s"Cannot read file ${file.getAbsolutePath}")
      return Future.value(ExitCode.DefinitionError)
    }
    val client = createAccioClient(env)
    JsonWorkflowParser.default
      .parse(file)
      .flatMap(workflow => client.validateWorkflow(ValidateWorkflowRequest(ThriftAdapter.toThrift(workflow))))
      .map(resp => handleResponse(resp.errors, resp.warnings, file, env.reporter))
  }

  private def handleResponse(errors: Seq[FieldViolation], warnings: Seq[FieldViolation], file: File, reporter: Reporter) = {
    warnings.foreach { violation =>
      reporter.warn(s"${violation.message} (at ${violation.field})")
    }
    errors.foreach { violation =>
      reporter.error(s"${violation.message} (at ${violation.field})")
    }
    if (errors.isEmpty) {
      reporter.info(s"Validated file ${file.getAbsolutePath}")
      ExitCode.Success
    } else {
      ExitCode.DefinitionError
    }
  }
}