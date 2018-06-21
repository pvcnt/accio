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

package fr.cnrs.liris.lumos.cli

import com.twitter.util.Future
import fr.cnrs.liris.infra.cli.app.{Environment, ExitCode}
import fr.cnrs.liris.lumos.domain.thrift.{ExecState, ThriftAdapter}
import fr.cnrs.liris.lumos.server.ListJobsRequest
import fr.cnrs.liris.util.StringUtils.padTo
import org.joda.time.Instant
import org.joda.time.format.DateTimeFormat

final class GetCommand extends LumosCommand {
  private[this] val allFlag = flag("all", false, "Show all resources, including those inactive")
  private[this] val labelsFlag = flag[String]("labels", "Show only resources including one of given labels")
  private[this] val ownerFlag = flag[String]("owner", "Show only resources belonging to a given owner")
  private[this] val limitFlag = flag[Int]("n", "Limit the number of shown resources")

  override def name = "get"

  override def execute(residue: Seq[String], env: Environment): Future[ExitCode] = {
    var states = Set[ExecState](ExecState.Pending, ExecState.Scheduled, ExecState.Running)
    if (allFlag()) {
      states ++= Set(ExecState.Failed, ExecState.Successful, ExecState.Canceled, ExecState.Lost)
    }
    val req = ListJobsRequest(
      owner = ownerFlag.get,
      labels = labelsFlag.get,
      state = Some(states),
      limit = limitFlag.get)
    val client = createLumosClient(env)
    client.listJobs(req).map { resp =>
      val columns = Seq(("ID", 32), ("CREATED", 15), ("STATUS", 9), ("LABELS", 30))
      columns.zipWithIndex.foreach { case ((name, width), idx) =>
        if (idx > 0) {
          env.reporter.outErr.printOut("  ")
        }
        env.reporter.outErr.printOut(padTo(name.toUpperCase, width))
      }
      env.reporter.outErr.printOutLn()

      val rows = resp.jobs.map(ThriftAdapter.toDomain).map { job =>
        Seq(job.name, humanize(job.createTime), job.status.state.name, job.labels.map { case (k, v) => s"$k=$v" }.mkString(", "))
      }
      rows.foreach { row =>
        columns.zipWithIndex.foreach { case ((_, width), idx) =>
          if (idx > 0) {
            env.reporter.outErr.printOut("  ")
          }
          env.reporter.outErr.printOut(padTo(row(idx), width))
        }
        env.reporter.outErr.printOutLn()
      }

      val moreJobs = if (resp.totalCount > resp.jobs.size) resp.totalCount - resp.jobs.size else 0
      if (moreJobs > 0) {
        env.reporter.outErr.printOutLn(s"$moreJobs more...")
      }

      ExitCode.Success
    }
  }

  private[this] val timeFormat = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm")

  private def humanize(time: Instant) = timeFormat.print(time)
}
