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

import java.nio.file.Files

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent.{AgentService$FinagleClient, ParseWorkflowRequest, PushWorkflowRequest}
import fr.cnrs.liris.accio.runtime.event.{Event, EventKind, Reporter}
import fr.cnrs.liris.accio.runtime.cli.{Cmd, ExitCode}
import fr.cnrs.liris.accio.core.domain.{Utils, WorkflowSpec}
import fr.cnrs.liris.common.flags.FlagsProvider
import fr.cnrs.liris.common.util.FileUtils

import scala.collection.JavaConverters._

@Cmd(
  name = "push",
  flags = Array(classOf[ClusterFlags]),
  help = "Push a workflow.",
  allowResidue = true)
class PushCommand @Inject()(clientProvider: ClusterClientProvider)
  extends ClientCommand(clientProvider) with StrictLogging {

  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      out.handle(Event.error("You must provide exactly at least one workflow definition file."))
      return ExitCode.CommandLineError
    }
    val client = createClient(flags)
    val outcomes = flags.residue.map(uri => parseAndPush(uri, client, out))
    ExitCode.select(outcomes)
  }

  private def parseAndPush(uri: String, client: AgentService$FinagleClient, out: Reporter): ExitCode = {
    val path = FileUtils.expandPath(uri)
    val file = path.toFile
    if (!file.exists || !file.canRead) {
      out.handle(Event.error(s"Cannot read workflow definition file: ${path.toAbsolutePath}"))
      ExitCode.DefinitionError
    } else {
      val content = Files.readAllLines(path).asScala.mkString
      val req = ParseWorkflowRequest(content, Some(path.getFileName.toString))
      handleResponse(client.parseWorkflow(req), out) { resp =>
        printErrors(resp.warnings, out, EventKind.Warning)
        printErrors(resp.errors, out, EventKind.Error)
        resp.workflow match {
          case Some(spec) => push(spec, client, out)
          case None =>
            out.handle(Event.error("Some errors where found in the workflow definition"))
            ExitCode.DefinitionError
        }
      }
    }
  }

  private def push(spec: WorkflowSpec, client: AgentService$FinagleClient, out: Reporter): ExitCode = {
    val req = PushWorkflowRequest(spec, Utils.DefaultUser)
    handleResponse(client.pushWorkflow(req), out) { _ =>
      out.handle(Event.info(s"Pushed workflow: ${spec.id.value}"))
      ExitCode.Success
    }
  }
}