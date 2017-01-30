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
import com.twitter.util.{Await, Duration, Return, Throw}
import fr.cnrs.liris.accio.agent.{GetRunRequest, ListRunsRequest}
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.common.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.StringUtils.padTo
import org.ocpsoft.prettytime.PrettyTime

case class InspectCommandFlags(
  @Flag(name = "json", help = "Print machine-readable JSON")
  json: Boolean = false)

@Cmd(
  name = "inspect",
  flags = Array(classOf[InspectCommandFlags], classOf[AccioAgentFlags]),
  help = "Retrieve execution status of a run.",
  allowResidue = true)
class InspectCommand @Inject()(clientFactory: AgentClientFactory) extends Command {
  private[this] val colWidth = 15

  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty || flags.residue.size > 2) {
      out.writeln("<error>[ERROR]</error> You must provide a single run identifier.")
      ExitCode.CommandLineError
    } else {
      val opts = flags.as[InspectCommandFlags]
      val req = GetRunRequest(RunId(flags.residue.head))
      val client = clientFactory.create(flags.as[AccioAgentFlags].addr)
      Await.result(client.getRun(req).liftToTry) match {
        case Return(resp) =>
          resp.result match {
            case None =>
              out.writeln(s"<error>[ERROR]</error> Unknown run: ${flags.residue.head}")
              ExitCode.CommandLineError
            case Some(run) =>
              if (flags.residue.size == 2) {
                run.state.nodes.find(_.name == flags.residue.last) match {
                  case None =>
                    out.writeln(s"<error>[ERROR]</error> Unknown node: ${flags.residue.last}")
                    ExitCode.CommandLineError
                  case Some(node) =>
                    if (opts.json) {
                      out.writeln(new String(new JsonSerializer().serialize(node)))
                    } else {
                      printNode(node, out)
                    }
                }
              } else {
                if (opts.json) {
                  val runWithoutResults = run.copy(state = run.state.copy(nodes = run.state.nodes.map(_.copy(result = None))))
                  out.writeln(new String(new JsonSerializer().serialize(runWithoutResults)))
                } else {
                  val children = if (run.children > 0) {
                    Await.result(client.listRuns(ListRunsRequest(parent = Some(run.id)))).results.sortBy(_.createdAt)
                  } else Seq.empty
                  printRun(run, children, out)
                }
              }
              ExitCode.Success
          }
        case Throw(e) =>
          out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
          ExitCode.InternalError
      }
    }
  }

  private def printRun(run: Run, children: Seq[Run], out: Reporter) = {
    val prettyTime = new PrettyTime().setLocale(Locale.ENGLISH)
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
      out.writeln("== Parameters ==")
      val maxLength = run.params.keySet.map(_.length).max
      run.params.foreach { case (name, value) =>
        out.writeln(s"<comment>${padTo(name, maxLength)}</comment> ${Values.toString(value)}")
      }
    }

    out.writeln()
    if (run.children == 0) {
      out.writeln("== Nodes ==")
      out.writeln(s"<comment>${padTo("Node name", 30)}  ${padTo("Status", 9)}  Duration</comment>")
      run.state.nodes.toSeq.sortBy(_.startedAt.getOrElse(Long.MaxValue)).foreach { node =>
        val duration = if (node.cacheHit) {
          "<cache hit>"
        } else if (node.startedAt.isDefined && node.completedAt.isDefined) {
          formatDuration(Duration.fromMilliseconds(node.completedAt.get - node.startedAt.get))
        } else {
          "-"
        }
        out.writeln(s"${padTo(node.name, 30)}  ${padTo(node.status.name, 9)}  $duration")
      }
    } else {
      out.writeln("== Child runs ==")
      val prettyTime = new PrettyTime().setLocale(Locale.ENGLISH)
      out.writeln(s"<comment>${padTo("Run id", 32)}  ${padTo("Created", 15)}  ${padTo("Run name", 15)}  Status</comment>")
      children.foreach { run =>
        val name = run.name.getOrElse("<no name>")
        out.writeln(s"${run.id.value}  ${padTo(prettyTime.format(new Date(run.createdAt)), 15)}  ${padTo(name, 15)}  ${run.state.status.name}")
      }
    }
  }

  private def printNode(node: NodeState, out: Reporter) = {
    val prettyTime = new PrettyTime().setLocale(Locale.ENGLISH)
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
        out.writeln(s"${padTo("Error class", colWidth)} ${error.root.classifier}")
        out.writeln(s"${padTo("Error message", colWidth)} ${error.root.message}")
        out.writeln(s"${padTo("Error stack", colWidth)} " +
          error.root.stacktrace.headOption.getOrElse("") + "\n" +
          error.root.stacktrace.tail.map(s => (" " * (colWidth + 1)) + s).mkString("\n"))
      }
      if (result.artifacts.nonEmpty) {
        out.writeln()
        out.writeln("== Artifacts ==")
        out.writeln(s"<comment>${padTo("Name", 25)}  Value (preview)</comment>")
        result.artifacts.foreach { artifact =>
          out.writeln(s"${padTo(artifact.name, 25)}  ${Values.toString(artifact.value)}</comment>")
        }
      }
      if (result.metrics.nonEmpty) {
        out.writeln()
        out.writeln("== Metrics ==")
        out.writeln(s"<comment>${padTo("Name", 25)}  Value</comment>")
        result.metrics.foreach { metric =>
          out.writeln(s"${padTo(metric.name, 25)}  ${metric.value}")
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
