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
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.api.{Utils, Values}
import fr.cnrs.liris.accio.tools.cli.event.Reporter
import fr.cnrs.liris.util.StringUtils.padTo

class DescribeJobController extends DescribeController[(Job, Seq[Job])] with FormatHelper {
  private[this] val colWidth = 15

  override def retrieve(id: String, client: AgentService.MethodPerEndpoint): Future[(Job, Seq[Job])] = {
    client
      .getJob(GetJobRequest(id))
      .flatMap { resp =>
        if (resp.job.status.children.isDefined) {
          client
            .listJobs(ListJobsRequest(parent = Some(resp.job.name)))
            .map(resp2 => (resp.job, resp2.jobs.sortBy(_.createTime)))
        } else {
          Future.value((resp.job, Seq.empty))
        }
      }
  }

  override def print(out: Reporter, resp: (Job, Seq[Job])): Unit = {
    val (job, children) = resp
    out.outErr.printOutLn(s"${padTo("Id", colWidth)} ${job.name}")
    job.parent.foreach { parentId =>
      out.outErr.printOutLn(s"${padTo("Parent Id", colWidth)} $parentId")
    }
    out.outErr.printOutLn(s"${padTo("Created", colWidth)} ${format(Time.fromMilliseconds(job.createTime))}")
    out.outErr.printOutLn(s"${padTo("Author", colWidth)} ${job.author.map(Utils.toString).getOrElse("<none>")}")
    out.outErr.printOutLn(s"${padTo("Title", colWidth)} ${job.title.getOrElse("<untitled>")}")
    out.outErr.printOutLn(s"${padTo("Tags", colWidth)} ${if (job.tags.nonEmpty) job.tags.mkString(", ") else "<none>"}")
    out.outErr.printOutLn(s"${padTo("Seed", colWidth)} ${job.seed}")
    out.outErr.printOutLn(s"${padTo("Status", colWidth)} ${job.status.state.name}")
    if (!Utils.isCompleted(job.status.state)) {
      out.outErr.printOutLn(s"${padTo("Progress", colWidth)} ${(job.status.progress * 100).round} %")
    }
    job.status.startTime.foreach { startTime =>
      out.outErr.printOutLn(s"${padTo("Started", colWidth)} ${format(Time.fromMilliseconds(startTime))}")
    }
    job.status.endTime.foreach { endTime =>
      out.outErr.printOutLn(s"${padTo("Completed", colWidth)} ${format(Time.fromMilliseconds(endTime))}")
      if (job.status.startTime.isDefined) {
        out.outErr.printOutLn(s"${padTo("Duration", colWidth)} " +
          format(Duration.fromMilliseconds(endTime - job.status.startTime.get)))
      }
    }

    if (job.params.nonEmpty) {
      out.outErr.printOutLn("NamedValues")
      val maxLength = job.params.map(_.name.length).max
      job.params.foreach { param =>
        out.outErr.printOutLn(s"  ${padTo(param.name, maxLength)} ${Values.stringify(param.value)}")
      }
    }

    out.outErr.printOutLn()
    if (job.status.tasks.isDefined) {
      out.outErr.printOutLn("Tasks")
      out.outErr.printOutLn(s"  ${padTo("Step name", 30)}  ${padTo("Status", 9)}  Duration")
      job.status.tasks.toSeq.flatten.sortBy(_.startTime.getOrElse(Long.MaxValue)).foreach { task =>
        val duration = if (task.startTime.isDefined && task.endTime.isDefined) {
          format(Duration.fromMilliseconds(task.endTime.get - task.startTime.get))
        } else {
          "-"
        }
        out.outErr.printOutLn(s"  ${padTo(task.name, 30)}  ${padTo(task.state.name, 9)}  $duration")
      }
    }
    if (job.status.children.isDefined) {
      out.outErr.printOutLn("Child Jobs")
      out.outErr.printOutLn(s"  ${padTo("ID", 32)}  ${padTo("CREATED", 15)}  ${padTo("NAME", 15)}  STATUS")
      children.foreach { child =>
        val title = child.title.getOrElse("<untitled>")
        out.outErr.printOutLn(s"  ${child.name}  ${padTo(format(Time.fromMilliseconds(child.createTime)), 15)}  ${padTo(title, 15)}  ${child.status.state.name}")
      }
    }
  }
}