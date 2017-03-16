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
import fr.cnrs.liris.accio.agent.{AgentService$FinagleClient, GetOperatorRequest, GetRunRequest, ListRunsRequest}
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.common.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.StringUtils
import fr.cnrs.liris.common.util.StringUtils.padTo
import fr.cnrs.liris.dal.core.api.{DataTypes, Values}
import org.ocpsoft.prettytime.PrettyTime

case class DescribeCommandFlags(
  @Flag(name = "output", help = "Output format")
  output: Option[String])

@Cmd(
  name = "describe",
  flags = Array(classOf[DescribeCommandFlags], classOf[CommonCommandFlags]),
  help = "Retrieve execution status of a run.",
  allowResidue = true)
class DescribeCommand @Inject()(clientProvider: ClusterClientProvider) extends Command {
  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.size < 2) {
      out.writeln(s"<error>[ERROR]</error> You must specify a resource type and identifier")
      return ExitCode.CommandLineError
    }
    val maybeController: Option[DescribeController[_]] = flags.residue.head match {
      case "run" | "runs" => Some(new DescribeRunController)
      case "node" | "nodes" => Some(new DescribeNodeController)
      case "operator" | "operators" | "op" | "ops" => Some(new DescribeOperatorController)
      case _ => None
    }
    maybeController match {
      case None =>
        out.writeln(s"<error>[ERROR]</error> Invalid resource type: ${flags.residue.head}")
        ExitCode.CommandLineError
      case Some(controller) =>
        val client = clientProvider(flags.as[CommonCommandFlags].cluster)
        val opts = flags.as[DescribeCommandFlags]
        execute(controller, out, flags.residue.last, opts, client)
    }
  }

  private def execute[Res](controller: DescribeController[Res], out: Reporter, id: String, opts: DescribeCommandFlags, client: AgentService$FinagleClient) = {
    val f = controller.retrieve(out, id, client).liftToTry
    Await.result(f) match {
      case Return(resp) =>
        controller.print(out, resp)
        ExitCode.Success
      case Throw(NoResultException()) =>
        out.writeln(s"<error>[ERROR]</error> No such resource: $id")
        ExitCode.CommandLineError
      case Throw(e) =>
        out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
        ExitCode.InternalError
    }
  }
}

case class NoResultException() extends Exception

trait DescribeController[Res] {
  def retrieve(out: Reporter, id: String, client: AgentService$FinagleClient): Future[Res]

  def print(out: Reporter, resp: Res): Unit
}

class DescribeRunController extends DescribeController[(Run, Seq[Run])] {
  private[this] val prettyTime = new PrettyTime().setLocale(Locale.ENGLISH)
  private[this] val colWidth = 15

  override def retrieve(out: Reporter, id: String, client: AgentService$FinagleClient): Future[(Run, Seq[Run])] = {
    client.getRun(GetRunRequest(RunId(id)))
      .flatMap { resp =>
        resp.result match {
          case None => throw new NoResultException
          case Some(run) =>
            if (run.children.nonEmpty) {
              client
                .listRuns(ListRunsRequest(parent = Some(run.id)))
                .map(resp2 => (run, resp2.results.sortBy(_.createdAt)))
            } else {
              Future.value((run, Seq.empty))
            }
        }
      }
  }

  override def print(out: Reporter, resp: (Run, Seq[Run])): Unit = {
    val (run, children) = resp
    out.writeln(s"${padTo("Id", colWidth)} ${run.id.value}")
    out.writeln(s"${padTo("Workflow", colWidth)} ${run.pkg.workflowId.value}:${run.pkg.workflowVersion}")
    out.writeln(s"${padTo("Created", colWidth)} ${prettyTime.format(new Date(run.createdAt))}")
    out.writeln(s"${padTo("Owner", colWidth)} ${Utils.toString(run.owner)}")
    out.writeln(s"${padTo("Name", colWidth)} ${run.name.getOrElse("<no name>")}")
    out.writeln(s"${padTo("Tags", colWidth)} ${if (run.tags.nonEmpty) run.tags.mkString(", ") else "<none>"}")
    out.writeln(s"${padTo("Seed", colWidth)} ${run.seed}")
    out.writeln(s"${padTo("Status", colWidth)} ${run.state.status.name}")
    if (!Utils.isCompleted(run.state.status)) {
      out.writeln(s"${padTo("Progress", colWidth)} ${(run.state.progress * 100).round} %")
    }
    run.state.startedAt.foreach { startedAt =>
      out.writeln(s"${padTo("Started", colWidth)} ${prettyTime.format(new Date(startedAt))}")
    }
    run.state.completedAt.foreach { completedAt =>
      out.writeln(s"${padTo("Completed", colWidth)} ${prettyTime.format(new Date(completedAt))}")
      if (run.state.startedAt.isDefined) {
        out.writeln(s"${padTo("Duration", colWidth)} " +
          formatDuration(Duration.fromMilliseconds(completedAt - run.state.startedAt.get)))
      }
    }

    if (run.params.nonEmpty) {
      out.writeln()
      out.writeln("Parameters")
      val maxLength = run.params.keySet.map(_.length).max
      run.params.foreach { case (name, value) =>
        out.writeln(s"  ${padTo(name, maxLength)} ${Values.toString(value)}")
      }
    }

    out.writeln()
    if (run.children.isEmpty) {
      out.writeln("Nodes")
      out.writeln(s"  ${padTo("Node name", 30)}  ${padTo("Status", 9)}  Duration")
      run.state.nodes.toSeq.sortBy(_.startedAt.getOrElse(Long.MaxValue)).foreach { node =>
        val duration = if (node.cacheHit) {
          "<cache hit>"
        } else if (node.startedAt.isDefined && node.completedAt.isDefined) {
          formatDuration(Duration.fromMilliseconds(node.completedAt.get - node.startedAt.get))
        } else {
          "-"
        }
        out.writeln(s"  ${padTo(node.name, 30)}  ${padTo(node.status.name, 9)}  $duration")
      }
    } else {
      out.writeln("Child Runs")
      val prettyTime = new PrettyTime().setLocale(Locale.ENGLISH)
      out.writeln(s"  ${padTo("ID", 32)}  ${padTo("CREATED", 15)}  ${padTo("NAME", 15)}  STATUS")
      children.foreach { run =>
        val name = run.name.getOrElse("<no name>")
        out.writeln(s"  ${run.id.value}  ${padTo(prettyTime.format(new Date(run.createdAt)), 15)}  ${padTo(name, 15)}  ${run.state.status.name}")
      }
    }
  }

  private def formatDuration(duration: Duration) = {
    if (duration.inHours > 5) {
      s"${duration.inHours} hours"
    } else if (duration.inMinutes > 30) {
      s"${duration.inMinutes} minutes"
    } else if (duration.inSeconds > 10) {
      s"${duration.inSeconds} seconds"
    } else {
      s"${duration.inMillis} milliseconds"
    }
  }
}

class DescribeNodeController extends DescribeController[NodeState] {
  private[this] val prettyTime = new PrettyTime().setLocale(Locale.ENGLISH)
  private[this] val colWidth = 15

  override def retrieve(out: Reporter, id: String, client: AgentService$FinagleClient): Future[NodeState] = {
    val parts = id.split("/")
    client.getRun(GetRunRequest(RunId(parts.head)))
      .map { resp =>
        resp.result match {
          case None => throw new NoResultException
          case Some(run) =>
            run.state.nodes.find(_.name == parts.last) match {
              case None => throw new NoResultException
              case Some(node) => node
            }
        }
      }
  }

  override def print(out: Reporter, node: NodeState): Unit = {
    out.writeln(s"${padTo("Node name", colWidth)} ${node.name}")
    out.writeln(s"${padTo("Status", colWidth)} ${node.status.name}")
    node.startedAt.foreach { startedAt =>
      out.writeln(s"${padTo("Started", colWidth)} ${prettyTime.format(new Date(startedAt))}")
    }
    node.completedAt.foreach { completedAt =>
      out.writeln(s"${padTo("Completed", colWidth)} ${prettyTime.format(new Date(completedAt))}")
      if (node.startedAt.isDefined) {
        out.write(s"${padTo("Duration", colWidth)} ")
        if (node.cacheHit) {
          out.writeln("<cache hit>")
        } else {
          out.writeln(formatDuration(Duration.fromMilliseconds(completedAt - node.startedAt.get)))
        }
      }
    }

    node.result.foreach { result =>
      out.writeln(s"${padTo("Exit code", colWidth)} ${result.exitCode}")
      result.error.foreach { error =>
        out.writeln()
        out.writeln(s"${padTo("Error class", colWidth)} <error>${error.root.classifier}</error>")
        out.writeln(s"<error>${padTo("Error message", colWidth)} <error>${error.root.message}</error>")
        out.writeln(s"${padTo("Error stack", colWidth)} " +
          error.root.stacktrace.headOption.getOrElse("") + "\n" +
          error.root.stacktrace.tail.map(s => (" " * (colWidth + 1)) + s).mkString("\n"))
      }
      if (result.artifacts.nonEmpty) {
        out.writeln()
        out.writeln("Artifacts")
        out.writeln(s"  ${padTo("Name", 25)}  Value (preview)")
        result.artifacts.foreach { artifact =>
          out.writeln(s"  ${padTo(artifact.name, 25)}  ${Values.toString(artifact.value)}")
        }
      }
      if (result.metrics.nonEmpty) {
        out.writeln()
        out.writeln("Metrics")
        out.writeln(s"  ${padTo("Name", 25)}  Value")
        result.metrics.foreach { metric =>
          out.writeln(s"  ${padTo(metric.name, 25)}  ${metric.value}")
        }
      }
    }
  }

  private def formatDuration(duration: Duration) = {
    if (duration.inHours > 5) {
      s"${duration.inHours} hours"
    } else if (duration.inMinutes > 30) {
      s"${duration.inMinutes} minutes"
    } else if (duration.inSeconds > 10) {
      s"${duration.inSeconds} seconds"
    } else {
      s"${duration.inMillis} milliseconds"
    }
  }
}

class DescribeOperatorController extends DescribeController[OpDef] {
  override def retrieve(out: Reporter, id: String, client: AgentService$FinagleClient): Future[OpDef] = {
    client.getOperator(GetOperatorRequest(id)).map { resp =>
      resp.result match {
        case None => throw new NoResultException
        case Some(opDef) => opDef
      }
    }
  }

  override def print(out: Reporter, opDef: OpDef): Unit = {
    out.writeln(s"Operator: ${opDef.name} (${opDef.category})")
    opDef.deprecation.foreach { deprecation =>
      out.writeln()
      out.writeln(s"<error>Deprecated: $deprecation</error>")
    }
    opDef.description.foreach { description =>
      out.writeln()
      out.writeln(StringUtils.paragraphFill(description, 80))
    }
    printInputs(out, opDef)
    printOutputs(out, opDef)
  }

  private def printInputs(out: Reporter, opDef: OpDef) = {
    out.writeln()
    out.writeln(s"Available inputs")
    opDef.inputs.foreach { argDef =>
      out.write(s"  ${argDef.name} (${DataTypes.toString(argDef.kind)}")
      if (argDef.defaultValue.isDefined) {
        out.write(s"; default: ${Values.toString(argDef.defaultValue.get)}")
      }
      if (argDef.isOptional) {
        out.write("; optional")
      }
      out.write(")")
      argDef.help.foreach(help => out.write(": " + help))
      out.writeln()
    }
  }

  private def printOutputs(out: Reporter, opDef: OpDef) = {
    out.writeln()
    if (opDef.outputs.nonEmpty) {
      out.writeln("Available outputs")
      opDef.outputs.foreach { outputDef =>
        out.write(s"  - ${outputDef.name} (${DataTypes.toString(outputDef.kind)})")
        out.writeln()
        outputDef.help.foreach(help => out.writeln(StringUtils.paragraphFill(help, 80, 4)))
      }
    }
  }
}