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

package fr.cnrs.liris.accio.state

import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.api.{Graph, Step, Utils}

import scala.collection.mutable

/**
 * Job state machine. It enforces the transitions for the tasks composing a job, and triggers
 * side-effect actions in response to state changes. Instances of this class are designed for a
 * single use, and should never be reused afterwards (which is why it is package private).
 *
 * @param _job    Job of interest. It has to be either a job without parent, or a child job
 *                (i.e., a job with actual tasks that can transition).
 * @param _parent Parent job, if any. If `_job` has a parent, it should be specified here.
 * @throws IllegalArgumentException If there is a mistmatch between the job and its parent.
 */
private[state] final class JobStateMachine(_job: Job, _parent: Option[Job]) extends Logging {
  require(_job.status.children.isEmpty, s"Job ${_job.name} should not be a parent job")
  require(
    _job.parent.forall(parentName => _parent.exists(_.name == parentName)),
    s"Job ${_parent.get.name} is not the parent of job ${_job.name}")

  import JobStateMachine._

  private class MutableJob(job: Job) {
    private[this] var updated = false
    private[this] var nextStatus = job.status

    def status: JobStatus = nextStatus

    def prevState: ExecState = job.status.state

    def nextState: ExecState = nextStatus.state

    def applyFollowup(): Unit = {
      if (updated) {
        addFollowup(SideEffect.Save(job.copy(status = nextStatus)))
      }
    }

    def update(status: Option[JobStatus]): Unit = status.foreach(update)

    def update(status: JobStatus): Unit = {
      nextStatus = status
      updated = true
    }
  }

  private[this] val job = new MutableJob(_job)
  private[this] val parent = _parent.map(new MutableJob(_))
  private[this] val graph = Graph.fromThrift(_job.steps)
  private[this] val sideEffects = mutable.ListBuffer.empty[SideEffect]

  def transitionTo(stepName: String, nextState: ExecState, result: Option[TaskResult] = None): StateChangeResult = {
    val task = getTask(stepName)
    val res = nextState match {
      case ExecState.Pending => Illegal
      case ExecState.Scheduled => setScheduled(task)
      case ExecState.Running => setRunning(task)
      case ExecState.Successful => setSuccessful(task, result)
      case ExecState.Failed => setFailed(task, result)
      case ExecState.Killed => setKilled(task)
      case ExecState.Cancelled => setCancelled(task)
      case ExecState.EnumUnknownExecState(_) => Illegal
    }
    toResult(res)
  }

  def schedule(): StateChangeResult = {
    if (job.status.state == ExecState.Pending) {
      graph.roots.foreach(step => setScheduled(getTask(step.name)))
      toResult(Success)
    } else if (job.status.state == ExecState.Successful) {
      toResult(Noop)
    } else {
      toResult(Illegal)
    }
  }

  def kill(): StateChangeResult = {
    if (Utils.isCompleted(job.status.state)) {
      // We consider killing a job to be an idempotent operation, and as such do not consider it
      // as illegal even if the job is already completed.
      StateChangeResult.Noop
    } else {
      tasks.filter(task => !Utils.isCompleted(task.state)).foreach(setKilled)
      toResult(Success)
    }
  }

  private def toResult(result: Result): StateChangeResult = {
    if (result == Success) {
      updateJobProgress()
      job.applyFollowup()

      updateParentProgress()
      parent.foreach(_.applyFollowup())
    }
    result match {
      case Success => StateChangeResult.Success(sideEffects.toList)
      case Illegal => StateChangeResult.Illegal(sideEffects.toList)
      case Noop =>
        if (sideEffects.nonEmpty) {
          logger.error("Noop result while side effects are provided is invalid")
        }
        StateChangeResult.Noop
    }
  }

  private def setRunning(task: Task): Result = {
    if (task.state == ExecState.Pending || task.state == ExecState.Successful) {
      replace(task.copy(startTime = Some(System.currentTimeMillis()), state = ExecState.Running))
      Success
    } else if (task.state == ExecState.Running) {
      Noop
    } else {
      Illegal
    }
  }

  private def setFailed(task: Task, result: Option[TaskResult]): Result = {
    if (task.state == ExecState.Running) {
      result match {
        case Some(TaskResult(exitCode, metrics, _)) =>
          replace(task.copy(endTime = Some(now()), state = ExecState.Failed, exitCode = Some(exitCode), metrics = Some(metrics)))
        case None =>
          replace(task.copy(endTime = Some(now()), state = ExecState.Failed))
      }
      cancelNextSteps(task.name)
      Success
    } else if (task.state == ExecState.Failed) {
      Noop
    } else {
      Illegal
    }
  }

  private def setSuccessful(task: Task, result: Option[TaskResult]): Result = {
    if (task.state == ExecState.Running) {
      result match {
        case Some(TaskResult(exitCode, metrics, artifacts)) =>
          replace(task.copy(endTime = Some(now()), state = ExecState.Successful, exitCode = Some(exitCode), metrics = Some(metrics), artifacts = Some(artifacts)))
        case None =>
          replace(task.copy(endTime = Some(now()), state = ExecState.Successful))
      }
      scheduleNextSteps(task.name)
      Success
    } else if (task.state == ExecState.Successful) {
      Noop
    } else {
      Illegal
    }
  }

  private def setKilled(task: Task): Result = {
    if (task.state == ExecState.Pending) {
      // Technically, it would be more correct to mark that task as cancelled, as it never ran.
      // However, it is more consistent to mark it as killed, and it also ensures that we never
      // end up with a job having only successful or cancelled tasks, which would be weird. That
      // way, we are certain that a killed job will always have at least a killed task.
      replace(task.copy(state = ExecState.Killed))
      cancelNextSteps(task.name)
      Success
    } else if (Utils.isActive(task.state)) {
      replace(task.copy(state = ExecState.Killed))
      addFollowup(SideEffect.Kill(task.name))
      cancelNextSteps(task.name)
      Success
    } else if (task.state == ExecState.Cancelled || task.state == ExecState.Killed) {
      Noop
    } else {
      Illegal
    }
  }

  private def setCancelled(task: Task): Result = {
    if (task.state == ExecState.Pending) {
      replace(task.copy(state = ExecState.Cancelled))
      cancelNextSteps(task.name)
      Success
    } else {
      Illegal
    }
  }

  private def setScheduled(task: Task): Result = {
    if (task.state == ExecState.Pending) {
      addFollowup(SideEffect.Schedule(graph(task.name)))
      replace(task.copy(state = ExecState.Scheduled))
      Success
    } else if (task.state == ExecState.Scheduled) {
      Noop
    } else {
      Illegal
    }
  }

  private def scheduleNextSteps(stepName: String): Unit = {
    getNextSteps(stepName)
      .filter(step => step.predecessors.forall(dep => getTask(dep).state == ExecState.Successful))
      .foreach(step => setScheduled(getTask(step.name)))
  }

  private def cancelNextSteps(stepName: String): Unit = {
    val nextSteps = getNextSteps(stepName).flatMap(step => Seq(step) ++ getNextSteps(step.name))
    nextSteps.foreach { step =>
      var task = tasks.find(_.name == step.name).get
      task = task.copy(endTime = Some(now()), state = ExecState.Cancelled)
      replace(task)
    }
  }

  private def replace(task: Task): Unit = {
    val idx = tasks.indexWhere(_.name == task.name)
    job.update(job.status.copy(tasks = Some(tasks.updated(idx, task))))
  }

  private def updateJobProgress(): Unit = {
    val states = tasks.groupBy(_.state).map { case (k, v) => k -> v.size }
    updateProgress(job, states)
  }

  private def updateParentProgress(): Unit = {
    parent.foreach { parent =>
      var children = parent.status.children.get
      children = children.updated(job.prevState, children(job.prevState) - 1)
      children = children.updated(job.nextState, children(job.nextState) + 1)
      parent.update(parent.status.copy(children = Some(children)))
      updateProgress(parent, children.toMap)
    }
  }

  private def updateProgress(job: MutableJob, states: Map[ExecState, Int]): Unit = {
    if (job.status.startTime.isEmpty) {
      if (states(ExecState.Running) > 0) {
        job.update(job.status.copy(startTime = Some(now()), state = ExecState.Running))
      } else if (states(ExecState.Scheduled) > 0) {
        job.update(job.status.copy(state = ExecState.Scheduled))
      }
    }
    if (job.status.endTime.isEmpty) {
      val total = states.values.sum
      val completed = states.filter { case (k, _) => Utils.isCompleted(k) }.values.sum
      if (completed == total) {
        // Mark job as completed, if all steps are completed.
        val state = if (states(ExecState.Successful) == total) {
          ExecState.Successful
        } else if (states(ExecState.Killed) > 0) {
          ExecState.Killed
        } else {
          ExecState.Failed
        }
        if (job.status.startTime.isEmpty) {
          job.update(job.status.copy(startTime = Some(now())))
        }
        val artifacts = graph.steps.flatMap { step =>
          step.exports.flatMap { export =>
            getTask(step.name)
              .artifacts
              .toSeq
              .flatten
              .find(_.name == export.output)
              .map(v => NamedValue(export.exportAs, v.value))
          }
        }
        tasks.foreach(task => replace(task.copy(artifacts = None)))
        // TODO: plan to do something to clean datasets on disk, or export the ones we keep to
        // a stable location.

        job.update(job.status.copy(
          progress = 1,
          state = state,
          artifacts = Some(artifacts),
          endTime = Some(now())))
      } else {
        // Job is not yet completed, only update progress.
        job.update(job.status.copy(progress = completed.toDouble / total))
      }
    }
  }

  private def getNextSteps(stepName: String): Set[Step] = {
    graph(stepName)
      .successors
      .map(graph.apply)
      .filter(step => getTask(step.name).state == ExecState.Pending)
  }

  private def tasks: Seq[Task] = job.status.tasks.toSeq.flatten

  private def getTask(stepName: String): Task = tasks.find(_.name == stepName).get

  private def addFollowup(sideEffect: SideEffect): Unit = sideEffects += sideEffect

  private def now() = System.currentTimeMillis()
}

object JobStateMachine {

  private sealed trait Result

  private case object Noop extends Result

  private case object Success extends Result

  private case object Illegal extends Result

}