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

package fr.cnrs.liris.accio.tools.cli.controller

import com.twitter.util.{Duration, Future, Time}
import fr.cnrs.liris.accio.agent.{AgentService, GetRunRequest}
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.api.{Errors, Values}
import fr.cnrs.liris.accio.tools.cli.event.Reporter
import fr.cnrs.liris.util.StringUtils.padTo

class DescribeNodeController extends DescribeController[NodeStatus] with FormatHelper {
  private[this] val colWidth = 15

  override def retrieve(id: String, client: AgentService.MethodPerEndpoint): Future[NodeStatus] = {
    val parts = id.split("/")
    client
      .getRun(GetRunRequest(parts.head))
      .map { resp =>
        resp.run.state.nodes.find(_.name == parts.last) match {
          case None => throw Errors.notFound("node", id)
          case Some(node) => node
        }
      }
  }

  override def print(out: Reporter, node: NodeStatus): Unit = {
    out.outErr.printOutLn(s"${padTo("Node name", colWidth)} ${node.name}")
    out.outErr.printOutLn(s"${padTo("Status", colWidth)} ${node.status.name}")
    node.startedAt.foreach { startedAt =>
      out.outErr.printOutLn(s"${padTo("Started", colWidth)} ${format(Time.fromMilliseconds(startedAt))}")
    }
    node.completedAt.foreach { completedAt =>
      out.outErr.printOutLn(s"${padTo("Completed", colWidth)} ${format(Time.fromMilliseconds(completedAt))}")
      if (node.startedAt.isDefined) {
        out.outErr.printOut(s"${padTo("Duration", colWidth)} ")
        out.outErr.printOutLn(format(Duration.fromMilliseconds(completedAt - node.startedAt.get)))
      }
    }

    node.result.foreach { result =>
      out.outErr.printOutLn(s"${padTo("Exit code", colWidth)} ${result.exitCode}")
      if (result.artifacts.nonEmpty) {
        out.outErr.printOutLn("Artifacts")
        out.outErr.printOutLn(s"  ${padTo("Name", 25)}  Value (preview)")
        result.artifacts.foreach { artifact =>
          out.outErr.printOutLn(s"  ${padTo(artifact.name, 25)}  ${Values.stringify(artifact.value)}")
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