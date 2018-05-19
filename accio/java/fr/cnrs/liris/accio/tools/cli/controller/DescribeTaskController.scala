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
import fr.cnrs.liris.accio.server.{AgentService, GetJobRequest}
import fr.cnrs.liris.accio.api.Errors
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.tools.cli.event.Reporter
import fr.cnrs.liris.util.StringUtils.padTo

class DescribeTaskController extends DescribeController[Task] with FormatHelper {
  private[this] val colWidth = 15

  override def retrieve(id: String, client: AgentService.MethodPerEndpoint): Future[Task] = {
    val parts = id.split("/")
    client
      .getJob(GetJobRequest(parts.head))
      .map { resp =>
        resp.job.status.tasks.toSeq.flatten.find(_.name == parts.last) match {
          case None => throw Errors.notFound("task", id)
          case Some(node) => node
        }
      }
  }

  override def print(out: Reporter, task: Task): Unit = {
    out.outErr.printOutLn(s"${padTo("Task name", colWidth)} ${task.name}")
    out.outErr.printOutLn(s"${padTo("Status", colWidth)} ${task.state.name}")
    task.startTime.foreach { startTime =>
      out.outErr.printOutLn(s"${padTo("Started", colWidth)} ${format(Time.fromMilliseconds(startTime))}")
    }
    task.endTime.foreach { endTime =>
      out.outErr.printOutLn(s"${padTo("Completed", colWidth)} ${format(Time.fromMilliseconds(endTime))}")
      if (task.startTime.isDefined) {
        out.outErr.printOut(s"${padTo("Duration", colWidth)} ")
        out.outErr.printOutLn(format(Duration.fromMilliseconds(endTime - task.startTime.get)))
      }
    }
    task.exitCode.foreach { exitCode =>
      out.outErr.printOutLn(s"${padTo("Exit code", colWidth)} $exitCode")
    }
    task.metrics.foreach { metrics =>
      out.outErr.printOutLn("Metrics")
      out.outErr.printOutLn(s"  ${padTo("Name", 25)}  Value")
      metrics.foreach { metric =>
        out.outErr.printOutLn(s"  ${padTo(metric.name, 25)}  ${metric.value} ${metric.unit.getOrElse("")}")
      }
    }
  }
}