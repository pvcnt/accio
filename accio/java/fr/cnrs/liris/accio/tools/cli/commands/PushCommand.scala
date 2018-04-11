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
import fr.cnrs.liris.accio.agent.PushWorkflowRequest
import fr.cnrs.liris.accio.api.thrift.Workflow
import fr.cnrs.liris.accio.dsl.WorkflowParser
import fr.cnrs.liris.accio.tools.cli.event.{Event, EventKind, Reporter}
import fr.cnrs.liris.util.FileUtils

final class PushCommand extends Command with ClientCommand {
  override def name = "push"

  override def help = "Push a workflow."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: CommandEnvironment): Future[ExitCode] = {
    if (residue.isEmpty) {
      env.reporter.handle(Event.error("You must provide exactly at least one workflow definition file."))
      return Future.value(ExitCode.CommandLineError)
    }
    val fs = residue.map(path => parseAndPush(path, env.reporter))
    Future.collect(fs).map(ExitCode.select)
  }

  private def parseAndPush(uri: String, reporter: Reporter): Future[ExitCode] = {
    val file = FileUtils.expandPath(uri).toFile
    if (!file.exists || !file.canRead) {
      reporter.handle(Event.error(s"Cannot read workflow definition file: ${file.getAbsolutePath}"))
      return Future.value(ExitCode.DefinitionError)
    }
    val parser = new WorkflowParser
    parser
      .parse(file)
      .flatMap(push(_, reporter))
  }

  private def push(workflow: Workflow, reporter: Reporter) = {
    client.pushWorkflow(PushWorkflowRequest(workflow)).map { resp =>
      resp.warnings.foreach { violation =>
        reporter.handle(Event(EventKind.Warning, s"${violation.message} (at ${violation.field})"))
      }
      reporter.handle(Event.info(s"Pushed workflow: ${resp.workflow.id}"))
      ExitCode.Success
    }
  }
}