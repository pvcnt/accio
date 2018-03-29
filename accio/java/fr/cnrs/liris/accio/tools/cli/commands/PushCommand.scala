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

import fr.cnrs.liris.accio.agent.{AgentService$FinagleClient, ParseWorkflowRequest, PushWorkflowRequest}
import fr.cnrs.liris.accio.api.Utils
import fr.cnrs.liris.accio.api.thrift.Workflow
import fr.cnrs.liris.accio.tools.cli.event.{Event, EventKind, Reporter}
import fr.cnrs.liris.common.util.FileUtils

import scala.collection.JavaConverters._

final class PushCommand extends Command with ClientCommand {
  override def name = "push"

  override def help = "Push a workflow."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: CommandEnvironment): ExitCode = {
    if (residue.isEmpty) {
      env.reporter.handle(Event.error("You must provide exactly at least one workflow definition file."))
      return ExitCode.CommandLineError
    }
    val outcomes = residue.map(uri => parseAndPush(uri, env.reporter))
    ExitCode.select(outcomes)
  }

  private def parseAndPush(uri: String, reporter: Reporter): ExitCode = {
    val path = FileUtils.expandPath(uri)
    val file = path.toFile
    if (!file.exists || !file.canRead) {
      reporter.handle(Event.error(s"Cannot read workflow definition file: ${path.toAbsolutePath}"))
      ExitCode.DefinitionError
    } else {
      val content = Files.readAllLines(path).asScala.mkString
      val req = ParseWorkflowRequest(content, Some(path.getFileName.toString))
      respond(client.parseWorkflow(req), reporter) { resp =>
        printErrors(resp.warnings, reporter, EventKind.Warning)
        printErrors(resp.errors, reporter, EventKind.Error)
        resp.workflow match {
          case Some(spec) => push(spec, client, reporter)
          case None =>
            reporter.handle(Event.error("Some errors where found in the workflow definition"))
            ExitCode.DefinitionError
        }
      }
    }
  }

  private def push(spec: Workflow, client: AgentService$FinagleClient, out: Reporter): ExitCode = {
    val req = PushWorkflowRequest(spec, Utils.DefaultUser)
    respond(client.pushWorkflow(req), out) { _ =>
      out.handle(Event.info(s"Pushed workflow: ${spec.id.value}"))
      ExitCode.Success
    }
  }
}