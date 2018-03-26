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

package fr.cnrs.liris.accio.tools.cli.command

import com.google.inject.Inject
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.tools.cli.controller._
import fr.cnrs.liris.accio.tools.cli.event.{Event, Reporter}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.StringUtils.explode

case class GetCommandFlags(
  @Flag(name = "all", help = "Show all resources, including those disabled")
  all: Boolean = false,
  @Flag(name = "tags", help = "Show only resources including one of given tags (comma-separated)")
  tags: Option[String],
  @Flag(name = "owner", help = "Show only resources with given owner")
  owner: Option[String],
  @Flag(name = "n", help = "Limit number of shown resources")
  n: Option[Int])

@Cmd(
  name = "get",
  flags = Array(classOf[GetCommandFlags], classOf[ClusterFlags]),
  help = "Display a list of resources.",
  allowResidue = true)
class GetCommand @Inject()(clientProvider: ClusterClientProvider) extends ClientCommand(clientProvider) {
  override def execute(flags: FlagsProvider, reporter: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      reporter.handle(Event.error("You must specify a resource type.\n" +
        "Valid resource types are: workflow, run, operator"))
      return ExitCode.CommandLineError
    }
    val maybeController: Option[GetController[_]] = flags.residue.head match {
      case "workflow" | "workflows" => Some(new GetWorkflowController)
      case "run" | "runs" => Some(new GetRunController)
      case "operator" | "operators" | "op" | "ops" => Some(new GetOperatorController)
      case _ => None
    }
    maybeController match {
      case None =>
        reporter.handle(Event.error(s"Invalid resource type: ${flags.residue.head}.\n" +
          s"Valid resource types are: workflow, run, operator"))
        ExitCode.CommandLineError
      case Some(controller) =>
        val client = createClient(flags)
        execute(controller, reporter, flags.as[GetCommandFlags], client)
    }
  }

  private def execute[Res](controller: GetController[Res], reporter: Reporter, opts: GetCommandFlags, client: AgentService$FinagleClient) = {
    val query = GetQuery(all = opts.all, owner = opts.owner, tags = explode(opts.tags, ","), limit = opts.n)
    handleResponse(controller.retrieve(query, client), reporter) { resp =>
      controller.print(reporter, resp)
      ExitCode.Success
    }
  }
}