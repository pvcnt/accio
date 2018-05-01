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

package fr.cnrs.liris.lumos.state

import java.util.UUID

import fr.cnrs.liris.lumos.domain.{Event, ExecStatus, Job, Task}
import org.joda.time.Instant

object JobStateMachine {

  sealed trait Result

  case class Ok(job: Job) extends Result

  case class Illegal(errors: Seq[String]) extends Result

  case object AlreadyExists extends Result

  def apply(job: Job, event: Event): Result =
    event.payload match {
      case e: Event.JobEnqueued => handleJobEnqueued(job, event.time, e)
      case e: Event.JobExpanded => handleJobExpanded(job, event.time, e)
      case Event.JobStarted => handleJobStarted(job, event.time)
      case e: Event.JobCompleted => handleJobCompleted(job, event.time, e)
      case Event.JobCanceled => handleJobCanceled(job, event.time)
      case e: Event.TaskStarted => handleTaskStarted(job, event.time, e)
      case e: Event.TaskCompleted => handleTaskCompleted(job, event.time, e)
    }

  private def handleJobEnqueued(job: Job, time: Instant, e: Event.JobEnqueued) = {
    if (job.name.nonEmpty) {
      AlreadyExists
    } else {
      // https://stackoverflow.com/questions/4267475/generating-8-character-only-uuids
      val name = if (job.name.isEmpty) UUID.randomUUID().getLeastSignificantBits.toHexString else job.name
      Ok(Job(
        name = name,
        createTime = time,
        owner = job.owner,
        contact = job.contact,
        labels = job.labels,
        metadata = job.metadata,
        inputs = job.inputs,
        status = ExecStatus(ExecStatus.Pending, time, reason = Some("Created"))))
    }
  }

  private def handleJobExpanded(job: Job, time: Instant, e: Event.JobExpanded) =
    job.status.state match {
      case ExecStatus.Pending | ExecStatus.Running =>
        val tasks = e.tasks.map { task =>
          Task(
            name = task.name,
            mnemonic = task.mnemonic,
            dependencies = task.dependencies,
            status = ExecStatus(ExecStatus.Pending, time, reason = Some("Created")))
        }
        Ok(job.copy(tasks = job.tasks ++ tasks))
      case _ => Illegal(Seq("Job is already completed"))
    }

  private def handleJobStarted(job: Job, time: Instant) =
    job.status.state match {
      case ExecStatus.Pending =>
        Ok(job.copy(
          history = job.history :+ job.status,
          status = ExecStatus(ExecStatus.Running, time)))
      case ExecStatus.Running => Illegal(Seq("Job is already running"))
      case _ => Illegal(Seq("Job is already completed"))
    }

  private def handleJobCompleted(job: Job, time: Instant, e: Event.JobCompleted) =
    ifRunning(job) {
      val states = job.tasks.map(_.status.state).toSet
      val state = if (states == Set(ExecStatus.Successful)) {
        ExecStatus.Successful
      } else if (states.contains(ExecStatus.Canceled)) {
        ExecStatus.Canceled
      } else {
        ExecStatus.Failed
      }
      Ok(job.copy(
        history = job.history :+ job.status,
        status = ExecStatus(state, time),
        outputs = e.outputs))
    }

  private def handleJobCanceled(job: Job, time: Instant) =
    ifRunning(job) {
      val tasks = job.tasks.map { task =>
        if (task.status.state.isCompleted) {
          task
        } else {
          task.copy(
            history = task.history :+ task.status,
            status = ExecStatus(ExecStatus.Canceled, time))
        }
      }
      Ok(job.copy(
        history = job.history :+ job.status,
        status = ExecStatus(ExecStatus.Canceled, time),
        tasks = tasks))
    }

  private def handleTaskStarted(job: Job, time: Instant, e: Event.TaskStarted) =
    ifRunning(job) {
      val idx = job.tasks.indexWhere(_.name == e.name)
      var task = job.tasks(idx)
      task.status.state match {
        case ExecStatus.Pending =>
          task = task.copy(
            history = task.history :+ task.status,
            status = ExecStatus(ExecStatus.Running, Instant.now()),
            links = e.links)
          Ok(job.copy(tasks = job.tasks.updated(idx, task)))
        case _ => Illegal(Seq("Task is already running or completed"))
      }
    }

  private def handleTaskCompleted(job: Job, time: Instant, e: Event.TaskCompleted) =
    ifRunning(job) {
      val idx = job.tasks.indexWhere(_.name == e.name)
      var task = job.tasks(idx)
      task.status.state match {
        case ExecStatus.Pending => Illegal(Seq("Task is not running"))
        case ExecStatus.Running =>
          val state = if (e.exitCode == 0) ExecStatus.Successful else ExecStatus.Failed
          if (state == ExecStatus.Successful && e.error.isDefined) {
            Illegal(Seq("A successful task cannot also define an error"))
          } else {
            task = task.copy(
              history = task.history :+ task.status,
              status = ExecStatus(state, Instant.now()),
              exitCode = Some(e.exitCode),
              metrics = e.metrics,
              error = e.error)
            val tasks = job.tasks.updated(idx, task)
            val progress = ((tasks.count(_.status.state.isCompleted).toDouble / tasks.size) * 100).round
            Ok(job.copy(tasks = tasks, progress = progress.toInt))
          }
        case _ => Illegal(Seq("Task is already completed"))
      }
    }

  private def ifRunning(job: Job)(fn: => Result) =
    job.status.state match {
      case ExecStatus.Pending => Illegal(Seq("Job is not running"))
      case ExecStatus.Running => fn
      case _ => Illegal(Seq("Job is already completed"))
    }
}