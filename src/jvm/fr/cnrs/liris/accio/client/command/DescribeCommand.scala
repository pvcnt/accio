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
import fr.cnrs.liris.accio.client.command.FormatUtils.format
import fr.cnrs.liris.accio.client.event.{Reporter, Event}
import fr.cnrs.liris.accio.client.runtime.{Cmd, ExitCode}
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.common.flags.FlagsProvider
import fr.cnrs.liris.common.util.StringUtils
import fr.cnrs.liris.common.util.StringUtils.padTo
import fr.cnrs.liris.dal.core.api.{DataTypes, Values}
import org.ocpsoft.prettytime.PrettyTime

case class DescribeCommandFlags()

//@Flag(name = "output", help = "Output format")
//output: Option[String]

@Cmd(
  name = "describe",
  flags = Array(classOf[DescribeCommandFlags], classOf[ClusterFlags]),
  help = "Retrieve execution status of a run.",
  allowResidue = true)
class DescribeCommand @Inject()(clientProvider: ClusterClientProvider) extends AbstractCommand(clientProvider) {
  override def execute(flags: FlagsProvider, reporter: Reporter): ExitCode = {
    if (flags.residue.size < 2) {
      reporter.handle(Event.error("You must specify a resource type and identifier"))
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

    val f = controller.retrieve(reporter, id, client).liftToTry
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

case class NoResultException() extends Exception

trait DescribeController[Res] {
  def retrieve(out: Reporter, id: String, client: AgentService$FinagleClient): Future[Res]

  def print(out: Reporter, resp: Res): Unit
}

class DescribeRunController extends DescribeController[(Run, Seq[Run])] {
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
    out.outErr.printOutLn(s"${padTo("Id", colWidth)} ${run.id.value}")
    run.parent.foreach { parentId =>
      out.outErr.printOutLn(s"${padTo("Parent Id", colWidth)} ${parentId.value}")
    }
    out.outErr.printOutLn(s"${padTo("Workflow", colWidth)} ${run.pkg.workflowId.value}:${run.pkg.workflowVersion}")
    out.outErr.printOutLn(s"${padTo("Created", colWidth)} ${format(Time.fromMilliseconds(run.createdAt))}")
    out.outErr.printOutLn(s"${padTo("Owner", colWidth)} ${Utils.toString(run.owner)}")
    out.outErr.printOutLn(s"${padTo("Name", colWidth)} ${run.name.getOrElse("<no name>")}")
    out.outErr.printOutLn(s"${padTo("Tags", colWidth)} ${if (run.tags.nonEmpty) run.tags.mkString(", ") else "<none>"}")
    out.outErr.printOutLn(s"${padTo("Seed", colWidth)} ${run.seed}")
    out.outErr.printOutLn(s"${padTo("Status", colWidth)} ${run.state.status.name}")
    if (!Utils.isCompleted(run.state.status)) {
      out.outErr.printOutLn(s"${padTo("Progress", colWidth)} ${(run.state.progress * 100).round} %")
    }
    run.state.startedAt.foreach { startedAt =>
      out.outErr.printOutLn(s"${padTo("Started", colWidth)} ${format(Time.fromMilliseconds(startedAt))}")
    }
    run.state.completedAt.foreach { completedAt =>
      out.outErr.printOutLn(s"${padTo("Completed", colWidth)} ${format(Time.fromMilliseconds(completedAt))}")
      if (run.state.startedAt.isDefined) {
        out.outErr.printOutLn(s"${padTo("Duration", colWidth)} " +
          format(Duration.fromMilliseconds(completedAt - run.state.startedAt.get)))
      }
    }

    if (run.params.nonEmpty) {
      out.outErr.printOutLn()
      out.outErr.printOutLn("Parameters")
      val maxLength = run.params.keySet.map(_.length).max
      run.params.foreach { case (name, value) =>
        out.outErr.printOutLn(s"  ${padTo(name, maxLength)} ${Values.toString(value)}")
      }
    }

    out.outErr.printOutLn()
    if (run.children.isEmpty) {
      out.outErr.printOutLn("Nodes")
      out.outErr.printOutLn(s"  ${padTo("Node name", 30)}  ${padTo("Status", 9)}  Duration")
      run.state.nodes.toSeq.sortBy(_.startedAt.getOrElse(Long.MaxValue)).foreach { node =>
        val duration = if (node.cacheHit) {
          "<cache hit>"
        } else if (node.startedAt.isDefined && node.completedAt.isDefined) {
          format(Duration.fromMilliseconds(node.completedAt.get - node.startedAt.get))
        } else {
          "-"
        }
        out.outErr.printOutLn(s"  ${padTo(node.name, 30)}  ${padTo(node.status.name, 9)}  $duration")
      }
    } else {
      out.outErr.printOutLn("Child Runs")
      val prettyTime = new PrettyTime().setLocale(Locale.ENGLISH)
      out.outErr.printOutLn(s"  ${padTo("ID", 32)}  ${padTo("CREATED", 15)}  ${padTo("NAME", 15)}  STATUS")
      children.foreach { run =>
        val name = run.name.getOrElse("<no name>")
        out.outErr.printOutLn(s"  ${run.id.value}  ${padTo(prettyTime.format(new Date(run.createdAt)), 15)}  ${padTo(name, 15)}  ${run.state.status.name}")
      }
    }
  }
}

class DescribeNodeController extends DescribeController[NodeState] {
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
    out.outErr.printOutLn(s"${padTo("Node name", colWidth)} ${node.name}")
    out.outErr.printOutLn(s"${padTo("Status", colWidth)} ${node.status.name}")
    node.startedAt.foreach { startedAt =>
      out.outErr.printOutLn(s"${padTo("Started", colWidth)} ${format(Time.fromMilliseconds(startedAt))}")
    }
    node.completedAt.foreach { completedAt =>
      out.outErr.printOutLn(s"${padTo("Completed", colWidth)} ${format(Time.fromMilliseconds(completedAt))}")
      if (node.startedAt.isDefined) {
        out.outErr.printOut(s"${padTo("Duration", colWidth)} ")
        if (node.cacheHit) {
          out.outErr.printOutLn("<cache hit>")
        } else {
          out.outErr.printOutLn(format(Duration.fromMilliseconds(completedAt - node.startedAt.get)))
        }
      }
    }

    node.result.foreach { result =>
      out.outErr.printOutLn(s"${padTo("Exit code", colWidth)} ${result.exitCode}")
      result.error.foreach { error =>
        out.outErr.printOutLn()
        out.outErr.printOutLn(s"${padTo("Error class", colWidth)} <error>${error.root.classifier}</error>")
        out.outErr.printOutLn(s"<error>${padTo("Error message", colWidth)} <error>${error.root.message}</error>")
        out.outErr.printOutLn(s"${padTo("Error stack", colWidth)} " +
          error.root.stacktrace.headOption.getOrElse("") + "\n" +
          error.root.stacktrace.tail.map(s => (" " * (colWidth + 1)) + s).mkString("\n"))
      }
      if (result.artifacts.nonEmpty) {
        out.outErr.printOutLn()
        out.outErr.printOutLn("Artifacts")
        out.outErr.printOutLn(s"  ${padTo("Name", 25)}  Value (preview)")
        result.artifacts.foreach { artifact =>
          out.outErr.printOutLn(s"  ${padTo(artifact.name, 25)}  ${Values.toString(artifact.value)}")
        }
      }
      if (result.metrics.nonEmpty) {
        out.outErr.printOutLn()
        out.outErr.printOutLn("Metrics")
        out.outErr.printOutLn(s"  ${padTo("Name", 25)}  Value")
        result.metrics.foreach { metric =>
          out.outErr.printOutLn(s"  ${padTo(metric.name, 25)}  ${metric.value}")
        }
      }
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
    out.outErr.printOutLn(s"Operator: ${opDef.name} (${opDef.category})")
    opDef.deprecation.foreach { deprecation =>
      out.outErr.printOutLn()
      out.outErr.printOutLn(s"<error>Deprecated: $deprecation</error>")
    }
    opDef.description.foreach { description =>
      out.outErr.printOutLn()
      out.outErr.printOutLn(StringUtils.paragraphFill(description, 80))
    }
    printInputs(out, opDef)
    printOutputs(out, opDef)
  }

  private def printInputs(out: Reporter, opDef: OpDef) = {
    out.outErr.printOutLn()
    out.outErr.printOutLn(s"Available inputs")
    opDef.inputs.foreach { argDef =>
      out.outErr.printOut(s"  ${argDef.name} (${DataTypes.toString(argDef.kind)}")
      if (argDef.defaultValue.isDefined) {
        out.outErr.printOut(s"; default: ${Values.toString(argDef.defaultValue.get)}")
      }
      if (argDef.isOptional) {
        out.outErr.printOut("; optional")
      }
      out.outErr.printOut(")")
      argDef.help.foreach(help => out.outErr.printOut(": " + help))
      out.outErr.printOutLn()
    }
  }

  private def printOutputs(out: Reporter, opDef: OpDef) = {
    out.outErr.printOutLn()
    if (opDef.outputs.nonEmpty) {
      out.outErr.printOutLn("Available outputs")
      opDef.outputs.foreach { outputDef =>
        out.outErr.printOut(s"  - ${outputDef.name} (${DataTypes.toString(outputDef.kind)})")
        out.outErr.printOutLn()
        outputDef.help.foreach(help => out.outErr.printOutLn(StringUtils.paragraphFill(help, 80, 4)))
      }
    }
  }
}