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

package fr.cnrs.liris.accio.client

import java.nio.file.Paths

import com.google.inject.Inject
import com.twitter.util.{Await, Stopwatch}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent.AgentService
import fr.cnrs.liris.accio.client.parser.WorkflowTemplateParser
import fr.cnrs.liris.accio.core.domain.Utils
import fr.cnrs.liris.accio.core.service.handler.PushWorkflowRequest
import fr.cnrs.liris.common.flags.FlagsProvider
import fr.cnrs.liris.common.util.TimeUtils

case class PushFlags()

@Cmd(
  name = "push",
  flags = Array(classOf[PushFlags]),
  help = "Push a workflow.",
  allowResidue = true)
class PushCommand @Inject()(agentClient: AgentService.FinagledClient, parser: WorkflowTemplateParser)
  extends Command with StrictLogging {

  def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.size != 1) {
      out.writeln("<error>You must provide exactly one workflow file.</error>")
      ExitCode.CommandLineError
    } else {
      val opts = flags.as[PushFlags]
      val elapsed = Stopwatch.start()

      val template = parser.parse(Paths.get(flags.residue.head))

      val req = PushWorkflowRequest(template, Utils.DefaultUser)
      val resp = Await.result(agentClient.pushWorkflow(req))

      out.writeln(s"Pushed workflow ${resp.workflow.id.value}:${resp.workflow.version}")

      out.writeln(s"Done in ${TimeUtils.prettyTime(elapsed())}.")
      ExitCode.Success
    }
  }
}