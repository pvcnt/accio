/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.client.command

import java.nio.file.{Files, Path}

import com.google.inject.Inject
import fr.cnrs.liris.accio.agent.{AgentService$FinagleClient, ParseRunRequest, ParseWorkflowRequest}
import fr.cnrs.liris.accio.runtime.event.{Event, EventKind, Reporter}
import fr.cnrs.liris.accio.runtime.cli.{Cmd, ExitCode}
import fr.cnrs.liris.common.flags.FlagsProvider
import fr.cnrs.liris.common.util.FileUtils

import scala.collection.JavaConverters._

@Cmd(
  name = "validate",
  help = "Validate the syntax and semantics of Accio definition files.",
  flags = Array(classOf[ClusterFlags]),
  allowResidue = true)
class ValidateCommand @Inject()(clientProvider: ClusterClientProvider) extends ClientCommand(clientProvider) {

  def execute(flags: FlagsProvider, reporter: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      reporter.handle(Event.error("You must specify at least one file to validate as argument"))
      return ExitCode.CommandLineError
    }
    val client = createClient(flags)
    val outcomes = flags.residue.map(uri => validate(uri, client, reporter))
    ExitCode.select(outcomes)
  }

  private def validate(uri: String, client: AgentService$FinagleClient, reporter: Reporter): ExitCode = {
    val path = FileUtils.expandPath(uri)
    if (!path.toFile.exists || !path.toFile.canRead) {
      reporter.handle(Event.error(s"Cannot read file ${path.toAbsolutePath}"))
      ExitCode.DefinitionError
    } else {
      val content = Files.readAllLines(path).asScala.mkString
      // Dirty detection of whether this file is a workflow or a run definition, based on JSON content.
      if (content.contains("\"workflow\"")) {
        validateRun(content, path, client, reporter)
      } else {
        validateWorkflow(content, path, client, reporter)
      }
    }
  }

  private def validateRun(content: String, path: Path, client: AgentService$FinagleClient, reporter: Reporter): ExitCode = {
    val req = ParseRunRequest(content, Map.empty, Some(path.getFileName.toString))
    handleResponse(client.parseRun(req), reporter) { resp =>
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

  private def validateWorkflow(content: String, path: Path, client: AgentService$FinagleClient, reporter: Reporter): ExitCode = {
    val req = ParseWorkflowRequest(content, Some(path.getFileName.toString))
    handleResponse(client.parseWorkflow(req), reporter) { resp =>
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