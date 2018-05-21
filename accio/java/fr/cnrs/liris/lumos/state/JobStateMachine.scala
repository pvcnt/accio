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

import fr.cnrs.liris.lumos.domain.Status.FieldViolation
import fr.cnrs.liris.lumos.domain._
import org.joda.time.Instant

object JobStateMachine {
  def apply(job: Job, event: Event): Either[Status, Job] =
    event.payload match {
      case e: Event.JobEnqueued => handleJobEnqueued(job, event.time, e)
      case e: Event.JobExpanded => handleJobExpanded(job, event.time, e)
      case e: Event.JobStarted => handleJobStarted(job, event.time, e)
      case e: Event.JobCompleted => handleJobCompleted(job, event.time, e)
      case e: Event.JobCanceled => handleJobCanceled(job, event.time, e)
      case e: Event.TaskStarted => handleTaskStarted(job, event.time, e)
      case e: Event.TaskCompleted => handleTaskCompleted(job, event.time, e)
    }

  private def handleJobEnqueued(job: Job, time: Instant, e: Event.JobEnqueued) = {
    if (job.name.nonEmpty) {
      Left(Status.AlreadyExists(job.name))
    } else {
      Right(Job(
        name = job.name,
        createTime = time,
        owner = job.owner,
        contact = job.contact,
        labels = job.labels,
        metadata = job.metadata,
        inputs = job.inputs,
        status = ExecStatus(ExecStatus.Pending, time, message = Some("Job created"))))
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
            metadata = task.metadata,
            status = ExecStatus(ExecStatus.Pending, time, message = Some("Job expanded")))
        }
        Right(job.copy(tasks = job.tasks ++ tasks))
      case _ => Left(Status.FailedPrecondition(job.name, Seq(FieldViolation("Job is already completed", "status.state"))))
    }

  private def handleJobStarted(job: Job, time: Instant, e: Event.JobStarted) =
    job.status.state match {
      case ExecStatus.Pending =>
        Right(job.copy(
          metadata = job.metadata ++ e.metadata,
          history = job.history :+ job.status,
          status = ExecStatus(ExecStatus.Running, time, e.message)))
      case ExecStatus.Running => Left(Status.FailedPrecondition(job.name, Seq(FieldViolation("Job is already running", "status.state"))))
      case _ => Left(Status.FailedPrecondition(job.name, Seq(FieldViolation("Job is already completed", "status.state"))))
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
      Right(job.copy(
        history = job.history :+ job.status,
        status = ExecStatus(state, time, e.message),
        outputs = e.outputs))
    }

  private def handleJobCanceled(job: Job, time: Instant, e: Event.JobCanceled) =
    ifRunning(job) {
      val tasks = job.tasks.map { task =>
        if (task.status.state.isCompleted) {
          task
        } else {
          task.copy(
            history = task.history :+ task.status,
            status = ExecStatus(ExecStatus.Canceled, time, e.message))
        }
      }
      Right(job.copy(
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
            metadata = task.metadata ++ e.metadata,
            history = task.history :+ task.status,
            status = ExecStatus(ExecStatus.Running, time, e.message),
            links = e.links)
          Right(job.copy(tasks = job.tasks.updated(idx, task)))
        case _ =>
          Left(Status.FailedPrecondition(job.name, Seq(FieldViolation("Task is already running or completed", s"tasks.$idx.status.state"))))
      }
    }

  private def handleTaskCompleted(job: Job, time: Instant, e: Event.TaskCompleted) =
    ifRunning(job) {
      val idx = job.tasks.indexWhere(_.name == e.name)
      var task = job.tasks(idx)
      task.status.state match {
        case ExecStatus.Pending =>
          Left(Status.FailedPrecondition(job.name, Seq(FieldViolation("Task is not running", s"tasks.$idx.status.state"))))
        case ExecStatus.Running =>
          val state = if (e.exitCode == 0) ExecStatus.Successful else ExecStatus.Failed
          if (state == ExecStatus.Successful && e.error.isDefined) {
            Left(Status.InvalidArgument(Seq(FieldViolation("A successful task cannot also define an error", s"error"))))
          } else {
            task = task.copy(
              history = task.history :+ task.status,
              status = ExecStatus(state, time, e.message),
              exitCode = Some(e.exitCode),
              metrics = e.metrics,
              error = e.error)
            val tasks = job.tasks.updated(idx, task)
            val progress = ((tasks.count(_.status.state.isCompleted).toDouble / tasks.size) * 100).round
            Right(job.copy(tasks = tasks, progress = progress.toInt))
          }
        case _ => Left(Status.FailedPrecondition(job.name, Seq(FieldViolation("Job is already completed", "status.state"))))
      }
    }

  private def ifRunning(job: Job)(fn: => Either[Status, Job]) =
    job.status.state match {
      case ExecStatus.Pending => Left(Status.FailedPrecondition(job.name, Seq(FieldViolation("Job is not running", "status.state"))))
      case ExecStatus.Running => fn
      case _ => Left(Status.FailedPrecondition(job.name, Seq(FieldViolation("Job is already completed", "status.state"))))
    }
}