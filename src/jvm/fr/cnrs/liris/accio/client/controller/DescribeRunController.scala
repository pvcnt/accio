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
import fr.cnrs.liris.accio.agent.{AgentService$FinagleClient, GetRunRequest, ListRunsRequest}
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.accio.runtime.event.Reporter
import fr.cnrs.liris.common.util.StringUtils.padTo
import fr.cnrs.liris.dal.core.api.Values

class DescribeRunController extends DescribeController[(Run, Seq[Run])] with FormatHelper {
  private[this] val colWidth = 15

  override def retrieve(id: String, client: AgentService$FinagleClient): Future[(Run, Seq[Run])] = {
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
      out.outErr.printOutLn(s"  ${padTo("ID", 32)}  ${padTo("CREATED", 15)}  ${padTo("NAME", 15)}  STATUS")
      children.foreach { run =>
        val name = run.name.getOrElse("<no name>")
        out.outErr.printOutLn(s"  ${run.id.value}  ${padTo(format(Time.fromMilliseconds(run.createdAt)), 15)}  ${padTo(name, 15)}  ${run.state.status.name}")
      }
    }
  }
}