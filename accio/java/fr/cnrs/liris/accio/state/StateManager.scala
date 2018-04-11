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

import java.util.concurrent.locks.ReentrantLock

import com.google.inject.{Inject, Singleton}
import com.twitter.util.Future
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.api.{OpRegistry, Step}
import fr.cnrs.liris.accio.scheduler.{Process, Scheduler}
import fr.cnrs.liris.accio.storage.Storage

@Singleton
final class StateManager @Inject()(scheduler: Scheduler, storage: Storage, opRegistry: OpRegistry) {
  private[this] val lock = new ReentrantLock

  private def locked[T](fn: => Future[T]): Future[T] = {
    lock.lock()
    fn.ensure(lock.unlock())
  }

  def schedule(job: Job, parent: Option[Job]): Future[Boolean] = locked {
    val stateMachine = new JobStateMachine(job, parent)
    applyResult(job, stateMachine.schedule())
  }

  def kill(job: Job, parent: Option[Job]): Future[Boolean] = locked {
    val stateMachine = new JobStateMachine(job, parent)
    applyResult(job, stateMachine.kill())
  }

  def delete(job: Job, parent: Option[Job]): Future[Boolean] = {
    kill(job, parent).flatMap {
      case false => Future.value(false)
      case true => storage.jobs.delete(job.name)
    }
  }

  def transitionTo(job: Job, parent: Option[Job], stepName: String, nextState: ExecState): Future[Boolean] = locked {
    val stateMachine = new JobStateMachine(job, parent)
    applyResult(job, stateMachine.transitionTo(stepName, nextState))
  }

  def transitionTo(job: Job, parent: Option[Job], stepName: String, nextState: ExecState, result: TaskResult): Future[Boolean] = locked {
    val stateMachine = new JobStateMachine(job, parent)
    applyResult(job, stateMachine.transitionTo(stepName, nextState, Some(result)))
  }

  private def applyResult(job: Job, result: StateChangeResult): Future[Boolean] = {
    result match {
      case StateChangeResult.Success(sideEffects) =>
        applySideEffects(job, sideEffects).map(_ => true)
      case StateChangeResult.Illegal(sideEffects) =>
        applySideEffects(job, sideEffects).map(_ => false)
      case StateChangeResult.Noop => Future.value(true)
    }
  }

  private def applySideEffects(job: Job, sideEffects: Seq[SideEffect]): Future[Unit] = {
    // TODO: should we react according to the result of the action?
    val fs = sideEffects.map {
      case SideEffect.Kill(processName) => scheduler.kill(processName)
      case SideEffect.Save(updatedJob) => storage.jobs.replace(updatedJob)
      case SideEffect.Schedule(step) =>
        scheduler.submit(createProcess(job, step))
        Future.Done
    }
    Future.join(fs)
  }

  private def createProcess(job: Job, step: Step): Process = {
    val opDef = opRegistry(step.op)
    val inputs = step.inputs.flatMap { case NamedChannel(portName, input) =>
      val maybeValue = input match {
        case Channel.Param(paramName) => job.params.find(_.name == paramName).map(_.value)
        case Channel.Reference(ref) =>
          job.status.tasks.toSeq.flatten
            .find(_.name == ref.step)
            .flatMap(node => node.artifacts.toSeq.flatten.find(_.name == ref.output))
            .map(_.value)
        case Channel.Value(v) => Some(v)
        case Channel.UnknownUnionField(_) => throw new IllegalArgumentException
      }
      maybeValue.map(value => NamedValue(portName, value))
    }
    val payload = OpPayload(opDef.name, job.seed, inputs, opDef.resource)
    Process(Process.name(job.name, step.name), payload)
  }
}
