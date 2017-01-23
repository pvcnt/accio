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
import fr.cnrs.liris.accio.core.domain.{JsonSerializer, RunStatus}
import fr.cnrs.liris.accio.core.infra.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.accio.core.service.handler.ListRunsRequest
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.StringUtils.{explode, padTo}
import org.ocpsoft.prettytime.PrettyTime

import scala.collection.mutable

case class PsFlags(
  @Flag(name = "q", help = "Print only identifiers")
  quiet: Boolean = false,
  @Flag(name = "json", help = "Print machine-readable JSON")
  json: Boolean = false,
  @Flag(name = "active", help = "Include active runs (scheduled or running)")
  active: Boolean = true,
  @Flag(name = "completed", help = "Include completed runs (successful or failed)", expansion = Array("success", "failed"))
  completed: Boolean = false,
  @Flag(name = "success", help = "Include successful runs")
  success: Boolean = false,
  @Flag(name = "failed", help = "Include failed runs")
  failed: Boolean = false,
  @Flag(name = "all", help = "Include all runs, whatever their status")
  all: Boolean = false,
  @Flag(name = "tags", help = "Filter by tags (comma-separated)")
  tags: Option[String],
  @Flag(name = "owner", help = "Filter by owner")
  owner: Option[String],
  @Flag(name = "name", help = "Filter by name")
  name: Option[String],
  @Flag(name = "n", help = "Maximum number of results")
  n: Option[Int])

@Cmd(
  name = "ps",
  flags = Array(classOf[PsFlags]),
  help = "List runs.",
  allowResidue = false)
class PsCommand @Inject()(client: AgentService.FinagledClient) extends Command {
  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    val opts = flags.as[PsFlags]

    val status = mutable.Set.empty[RunStatus]
    if (!opts.all) {
      if (opts.success) {
        status += RunStatus.Success
      }
      if (opts.failed) {
        status ++= Set(RunStatus.Failed, RunStatus.Killed)
      }
      if (opts.active) {
        status ++= Set(RunStatus.Scheduled, RunStatus.Running)
      }
    }
    val n = opts.n.getOrElse(100)
    val req = ListRunsRequest(
      owner = opts.owner,
      name = opts.name,
      tags = opts.tags.map(tags => explode(tags)),
      status = if (status.nonEmpty) Some(status.toSet) else None,
      limit = Some(n))
    val f = client.listRuns(req).liftToTry

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
            out.writeln(s"<comment>${padTo("Run id", 32)}  ${padTo("Workflow id", 15)}  ${padTo("Created", 15)}  ${padTo("Run name", 15)}  ${padTo("Status", 9)}  Nodes</comment>")
            resp.results.foreach { run =>
              out.writeln(s"${run.id.value}  ${padTo(run.pkg.workflowId.value, 15)}  ${padTo(prettyTime.format(new Date(run.createdAt)), 15)}  ${padTo(run.name.getOrElse("<no name>"), 15)}  ${padTo(run.state.status.name, 9)}  ${run.state.nodes.size}")
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
