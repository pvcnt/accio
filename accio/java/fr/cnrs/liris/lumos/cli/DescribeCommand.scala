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
import fr.cnrs.liris.lumos.domain.{DataType, RemoteFile, Value}
import fr.cnrs.liris.lumos.domain.thrift.ThriftAdapter
import fr.cnrs.liris.lumos.server.GetJobRequest
import fr.cnrs.liris.util.StringUtils.padTo
import org.joda.time.format.DateTimeFormat
import org.joda.time.{Duration, Instant}

final class DescribeCommand extends LumosCommand {
  private[this] val colWidth = 15

  override def name = "describe"

  override def allowResidue = true

  override def execute(residue: Seq[String], env: Environment): Future[ExitCode] = {
    if (residue.isEmpty) {
      env.reporter.warn("Specify the identifier of a job to describe.")
      Future.value(ExitCode.CommandLineError)
    } else {
      val client = createLumosClient(env)
      client.getJob(GetJobRequest(residue.head)).map { resp =>
        val job = ThriftAdapter.toDomain(resp.job)
        env.reporter.outErr.printOutLn(s"${padTo("Id", colWidth)} ${job.name}")
        env.reporter.outErr.printOutLn(s"${padTo("Created", colWidth)} ${humanize(job.createTime)}")
        env.reporter.outErr.printOutLn(s"${padTo("Owner", colWidth)} ${job.owner.getOrElse("<none>")}")
        env.reporter.outErr.printOutLn(s"${padTo("Labels", colWidth)} ${if (job.labels.nonEmpty) job.labels.map { case (k, v) => s"$k=$v" }.mkString(", ") else "<none>"}")
        env.reporter.outErr.printOutLn(s"${padTo("Metadata", colWidth)} ${if (job.metadata.nonEmpty) job.labels.map { case (k, v) => s"$k=$v" }.mkString(", ") else "<none>"}")
        env.reporter.outErr.printOutLn(s"${padTo("Status", colWidth)} ${job.status.state.name}")
        job.startTime.foreach { startTime =>
          env.reporter.outErr.printOutLn(s"${padTo("Started", colWidth)} ${humanize(startTime)}")
        }
        job.endTime.foreach { endTime =>
          env.reporter.outErr.printOutLn(s"${padTo("Completed", colWidth)} ${humanize(endTime)}")
        }
        env.reporter.outErr.printOutLn(s"${padTo("Duration", colWidth)} ${humanize(job.duration)}")
        if (!job.status.state.isCompleted && job.progress > 0) {
          env.reporter.outErr.printOutLn(s"${padTo("Progress", colWidth)} ${job.progress} %")
        }

        if (job.inputs.nonEmpty) {
          env.reporter.outErr.printOutLn("Inputs")
          val maxLength = job.inputs.map(_.name.length).max
          job.inputs.foreach { attr =>
            env.reporter.outErr.printOutLn(s"  ${padTo(attr.name, maxLength)} ${humanize(attr.value)}")
          }
        }

        if (job.outputs.nonEmpty) {
          env.reporter.outErr.printOutLn("Outputs")
          val maxLength = job.outputs.map(_.name.length).max
          job.outputs.foreach { attr =>
            env.reporter.outErr.printOutLn(s"  ${padTo(attr.name, maxLength)} ${humanize(attr.value)}")
          }
        }

        env.reporter.outErr.printOutLn()
        if (job.tasks.nonEmpty) {
          env.reporter.outErr.printOutLn("Tasks")
          env.reporter.outErr.printOutLn(s"  ${padTo("TASK NAME", 30)}  ${padTo("STATUS", 10)}  DURATION")
          job.tasks.foreach { task =>
            env.reporter.outErr.printOutLn(s"  ${padTo(task.name, 30)}  ${padTo(task.status.state.name, 10)}  ${humanize(task.duration)}")
          }
        }
        ExitCode.Success
      }
    }
  }

  private def humanize(value: Value): String = {
    value.cast(DataType.File) match {
      case Some(v) =>
        // If the value can be viewed as a file, we display its directly its URI.
        v.v.asInstanceOf[RemoteFile].uri
      case _ => value.v.toString
    }
  }

  private[this] val timeFormat = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm")

  private def humanize(time: Instant) = timeFormat.print(time)

  private def humanize(duration: Duration): String = {
    val ms = duration.getMillis.toDouble
    if (ms < 10.0) {
      f"$ms%.2f ms"
    } else if (ms < 100.0) {
      f"$ms%.1f ms"
    } else if (ms < 1000.0) {
      f"$ms%.0f ms"
    } else {
      f"${ms / 1000}%.3f s"
    }
  }
}
