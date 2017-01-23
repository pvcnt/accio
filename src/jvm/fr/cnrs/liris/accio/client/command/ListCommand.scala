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

import java.util.{Date, Locale}

import com.google.inject.Inject
import com.twitter.util.{Await, Return, Throw}
import fr.cnrs.liris.accio.agent.AgentService
import fr.cnrs.liris.accio.core.domain.JsonSerializer
import fr.cnrs.liris.accio.core.infra.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.accio.core.service.handler.ListWorkflowsRequest
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.StringUtils.padTo
import org.ocpsoft.prettytime.PrettyTime

case class ListFlags(
  @Flag(name = "q", help = "Print only identifiers")
  quiet: Boolean = false,
  @Flag(name = "json", help = "Print machine-readable JSON")
  json: Boolean = false,
  @Flag(name = "owner", help = "Filter by owner")
  owner: Option[String],
  @Flag(name = "name", help = "Filter by name")
  name: Option[String],
  @Flag(name = "n", help = "Maximum number of results")
  n: Option[Int])

@Cmd(
  name = "list",
  flags = Array(classOf[ListFlags]),
  help = "List workflows.",
  allowResidue = false)
class ListCommand @Inject()(client: AgentService.FinagledClient) extends Command {
  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    val opts = flags.as[ListFlags]

    val n = opts.n.getOrElse(100)
    val req = ListWorkflowsRequest(owner = opts.owner, name = opts.name, limit = Some(n))
    val f = client.listWorkflows(req).liftToTry

    Await.result(f) match {
      case Return(resp) =>
        if (opts.quiet) {
          if (opts.json) {
            out.writeln("[" + resp.results.map(_.id.value).mkString(",") + "]")
          } else {
            resp.results.map(_.id.value).foreach(out.writeln)
          }
        } else {
          if (opts.json) {
            val serializer = new JsonSerializer
            out.writeln("[" + resp.results.map(serializer.serialize).mkString(",") + "]")
          } else {
            val prettyTime = new PrettyTime().setLocale(Locale.ENGLISH)
            out.writeln(s"<comment>${padTo("Id", 30)}  ${padTo("Owner", 15)}  ${padTo("Created", 15)}  Name</comment>")
            resp.results.foreach { workflow =>
              out.writeln(s"${padTo(workflow.id.value, 30)}  ${padTo(workflow.owner.name, 15)}  ${padTo(prettyTime.format(new Date(workflow.createdAt)), 15)}  ${workflow.name.getOrElse("<no name>")}")
            }
            if (resp.totalCount > n) {
              out.writeln(s"${resp.totalCount - n} more...")
            }
          }
        }
        ExitCode.Success
      case Throw(e) =>
        if (!opts.quiet) {
          out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
        }
        ExitCode.InternalError
    }
  }
}
