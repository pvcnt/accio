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
import com.twitter.util._
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.core.domain.RunStatus
import fr.cnrs.liris.common.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.StringUtils.{explode, padTo}
import org.ocpsoft.prettytime.PrettyTime

import scala.collection.mutable

case class GetCommandFlags(
  //@Flag(name = "output", help = "Output format")
  //output: Option[String],
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
  help = "List runs.",
  allowResidue = true)
class GetCommand @Inject()(clientProvider: ClusterClientProvider) extends Command {
  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      out.writeln(s"<error>[ERROR]</error> You must specify a resource type")
      return ExitCode.CommandLineError
    }
    val maybeController: Option[GetController[_]] = flags.residue.head match {
      case "workflow" | "workflows" => Some(new GetWorkflowController)
      case "run" | "runs" => Some(new GetRunController)
      case "operator" | "operators" | "op" | "ops" => Some(new GetOperatorController)
      case "agent" | "agents" => Some(new GetAgentController)
      case _ => None
    }
    maybeController match {
      case None =>
        out.writeln(s"<error>[ERROR]</error> Invalid resource type: ${flags.residue.head}")
        ExitCode.CommandLineError
      case Some(controller) =>
        val client = clientProvider(flags.as[ClusterFlags].cluster)
        val opts = flags.as[GetCommandFlags]
        execute(controller, out, opts, client)
    }
  }

  private def execute[Res](controller: GetController[Res], out: Reporter, opts: GetCommandFlags, client: AgentService$FinagleClient) = {
    val f = controller.retrieve(out, opts, client).liftToTry
    Await.result(f) match {
      case Return(resp) =>
        controller.print(out, resp)
        ExitCode.Success
      case Throw(e) =>
        out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
        ExitCode.InternalError

    }
  }
}

trait GetController[Res] {
  def retrieve(out: Reporter, opts: GetCommandFlags, client: AgentService$FinagleClient): Future[Res]

  def print(out: Reporter, resp: Res): Unit
}

class GetRunController extends GetController[ListRunsResponse] {
  private[this] val prettyTime = new PrettyTime().setLocale(Locale.ENGLISH)

  override def retrieve(out: Reporter, opts: GetCommandFlags, client: AgentService$FinagleClient): Future[ListRunsResponse] = {
    val status = Set[RunStatus](RunStatus.Scheduled, RunStatus.Running) ++
      (if (opts.all) Set(RunStatus.Failed, RunStatus.Success, RunStatus.Killed) else Set.empty)
    val tags = opts.tags.map(explode(",", _))
    val req = ListRunsRequest(owner = opts.owner, tags = tags, status = Some(status), limit = opts.n)
    client.listRuns(req)
  }

  override def print(out: Reporter, resp: ListRunsResponse): Unit = {
    out.writeln(s"${padTo("ID", 32)}  ${padTo("WORKFLOW", 15)}  ${padTo("CREATED", 15)}  ${padTo("NAME", 30)}  STATUS")
    resp.results.foreach { run =>
      val name = (if (run.children.nonEmpty) s"(${run.children.size}) " else "") + run.name.getOrElse("<no name>")
      out.writeln(s"${padTo(run.id.value, 32)}  ${padTo(run.pkg.workflowId.value, 15)}  ${padTo(prettyTime.format(new Date(run.createdAt)), 15)}  ${padTo(name, 30)}  ${run.state.status.name}")
    }
    if (resp.totalCount > resp.results.size) {
      out.writeln(s"${resp.totalCount - resp.results.size} more...")
    }
  }
}

class GetOperatorController extends GetController[ListOperatorsResponse] {
  override def retrieve(out: Reporter, opts: GetCommandFlags, client: AgentService$FinagleClient): Future[ListOperatorsResponse] = {
    client.listOperators(ListOperatorsRequest(includeDeprecated = opts.all))
  }

  override def print(out: Reporter, resp: ListOperatorsResponse): Unit = {
    val maxLength = resp.results.map(_.name.length).max
    resp.results.sortBy(_.name).groupBy(_.category).foreach { case (category, categoryOps) =>
      out.writeln(s"Operators in $category category")
      categoryOps.foreach { op =>
        val padding = " " * (maxLength - op.name.length)
        out.writeln(s"  ${op.name}$padding ${op.help.getOrElse("")}")
      }
      out.writeln()
    }
  }
}

class GetWorkflowController extends GetController[ListWorkflowsResponse] {
  private[this] val prettyTime = new PrettyTime().setLocale(Locale.ENGLISH)

  override def retrieve(out: Reporter, opts: GetCommandFlags, client: AgentService$FinagleClient): Future[ListWorkflowsResponse] = {
    client.listWorkflows(ListWorkflowsRequest(owner = opts.owner, limit = opts.n))
  }

  override def print(out: Reporter, resp: ListWorkflowsResponse): Unit = {
    out.writeln(s"${padTo("ID", 30)}  ${padTo("OWNER", 15)}  ${padTo("CREATED", 15)}  NAME")
    resp.results.foreach { workflow =>
      out.writeln(s"${padTo(workflow.id.value, 30)}  ${padTo(workflow.owner.name, 15)}  ${padTo(prettyTime.format(new Date(workflow.createdAt)), 15)}  ${workflow.name.getOrElse("<no name>")}")
    }
    if (resp.totalCount > resp.results.size) {
      out.writeln(s"${resp.totalCount - resp.results.size} more...")
    }
  }
}

class GetAgentController extends GetController[ListAgentsResponse] {
  override def retrieve(out: Reporter, opts: GetCommandFlags, client: AgentService$FinagleClient): Future[ListAgentsResponse] = {
    client.listAgents(ListAgentsRequest())
  }

  override def print(out: Reporter, resp: ListAgentsResponse): Unit = {
    out.writeln(s"${padTo("NAME", 20)}  ${padTo("CPU", 4)}  ${padTo("RAM", 8)}  ${padTo("DISK", 8)}  TYPE")
    resp.results.foreach { agent =>
      val types = mutable.Set.empty[String]
      if (agent.isMaster) {
        types += "Master"
      }
      if (agent.isWorker) {
        types += "Worker"
      }
      out.write(s"${padTo(agent.id.value, 20)}  ")
      if (agent.isWorker) {
        out.write(s"${padTo(agent.maxResources.cpu.toString, 4)}  ${padTo(formatStorage(agent.maxResources.ramMb), 8)}  ${padTo(formatStorage(agent.maxResources.diskMb), 8)}")
      } else {
        out.write(s"${padTo("-", 4)}  ${padTo("-", 8)}  ${padTo("-", 8)}")
      }
      out.writeln(s"  ${types.mkString(",")}")
    }
  }

  private def formatStorage(valueMb: Long) = {
    StorageUnit.fromMegabytes(valueMb).toHuman.replace(" ", "")
  }
}