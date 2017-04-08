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

package fr.cnrs.liris.accio.client.controller

import com.twitter.util.{Duration, Future, Time}
import fr.cnrs.liris.accio.agent.{AgentService$FinagleClient, GetRunRequest}
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.runtime.event.Reporter
import fr.cnrs.liris.common.util.StringUtils.padTo
import fr.cnrs.liris.dal.core.api.Values

class DescribeNodeController extends DescribeController[NodeState] with FormatHelper {
  private[this] val colWidth = 15

  override def retrieve(id: String, client: AgentService$FinagleClient): Future[NodeState] = {
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
        out.outErr.printOutLn(s"${padTo("Error class", colWidth)} <error>${error.root.classifier}</error>")
        out.outErr.printOutLn(s"<error>${padTo("Error message", colWidth)} <error>${error.root.message}</error>")
        out.outErr.printOutLn(s"${padTo("Error stack", colWidth)} " +
          error.root.stacktrace.headOption.getOrElse("") + "\n" +
          error.root.stacktrace.tail.map(s => (" " * (colWidth + 1)) + s).mkString("\n"))
      }
      if (result.artifacts.nonEmpty) {
        out.outErr.printOutLn("Artifacts")
        out.outErr.printOutLn(s"  ${padTo("Name", 25)}  Value (preview)")
        result.artifacts.foreach { artifact =>
          out.outErr.printOutLn(s"  ${padTo(artifact.name, 25)}  ${Values.toString(artifact.value)}")
        }
      }
      if (result.metrics.nonEmpty) {
         out.outErr.printOutLn("Metrics")
        out.outErr.printOutLn(s"  ${padTo("Name", 25)}  Value")
        result.metrics.foreach { metric =>
          out.outErr.printOutLn(s"  ${padTo(metric.name, 25)}  ${metric.value}")
        }
      }
    }
  }
}