/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.tools.cli.command

import com.google.inject.Inject
import com.twitter.util._
import fr.cnrs.liris.accio.tools.cli.controller._
import fr.cnrs.liris.accio.agent.AgentService$FinagleClient
import fr.cnrs.liris.accio.runtime.cli.{Cmd, ExitCode}
import fr.cnrs.liris.accio.runtime.event.{Event, Reporter}
import fr.cnrs.liris.common.flags.FlagsProvider

case class DescribeCommandFlags()

@Cmd(
  name = "describe",
  flags = Array(classOf[DescribeCommandFlags], classOf[ClusterFlags]),
  help = "Display details about a specific resource.",
  allowResidue = true)
class DescribeCommand @Inject()(clientProvider: ClusterClientProvider) extends ClientCommand(clientProvider) {

  override def execute(flags: FlagsProvider, reporter: Reporter): ExitCode = {
    if (flags.residue.size < 2) {
      reporter.handle(Event.error("You must specify a resource type and identifier"))
      return ExitCode.CommandLineError
    }
    val maybeController: Option[DescribeController[_]] = flags.residue.head match {
      case "run" | "runs" => Some(new DescribeRunController)
      case "node" | "nodes" => Some(new DescribeNodeController)
      case "workflow" | "workflows" => Some(new DescribeWorkflowController)
      case "operator" | "operators" | "op" | "ops" => Some(new DescribeOperatorController)
      case _ => None
    }
    maybeController match {
      case None =>
        reporter.handle(Event.error(s"Invalid resource type: ${flags.residue.head}"))
        ExitCode.CommandLineError
      case Some(controller) =>
        val opts = flags.as[DescribeCommandFlags]
        execute(controller, reporter, flags.residue.last, opts, createClient(flags))
    }
  }

  private def execute[Res](
    controller: DescribeController[Res],
    reporter: Reporter,
    id: String,
    opts: DescribeCommandFlags,
    client: AgentService$FinagleClient) = {

    val f = controller.retrieve(id, client).liftToTry
    Await.result(f) match {
      case Return(resp) =>
        controller.print(reporter, resp)
        ExitCode.Success
      case Throw(NoResultException()) =>
        reporter.handle(Event.error(s"No such resource: $id"))
        ExitCode.CommandLineError
      case Throw(e) =>
        reporter.error(s"Server error", e)
        ExitCode.InternalError
    }
  }
}