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

package fr.cnrs.liris.accio.agent

import com.google.common.eventbus.Subscribe
import com.google.inject.{Inject, Singleton}
import com.twitter.util.logging.Logging
import com.twitter.util.{Await, Future}
import fr.cnrs.liris.accio.api.thrift.ExecState
import fr.cnrs.liris.accio.api.{ProcessCompletedEvent, ProcessStartedEvent}
import fr.cnrs.liris.accio.state.{StateManager, TaskResult}
import fr.cnrs.liris.accio.storage.Storage

/**
 *
 * @param storage      Storage.
 * @param stateManager State manager.
 */
@Singleton
final class SchedulerListener @Inject()(storage: Storage, stateManager: StateManager)
  extends Logging {

  @Subscribe
  def onProcessStarted(event: ProcessStartedEvent): Unit = {
    Await.result(storage.jobs.get(event.jobName).flatMap {
      case None =>
        logger.warn(s"Started process associated with unknown job ${event.jobName}")
        Future.Done
      case Some(job) =>
        job.parent match {
          case Some(parentName) =>
            storage.jobs.get(parentName).flatMap { parent =>
              stateManager.transitionTo(job, parent, event.taskName, ExecState.Running)
            }
          case None => stateManager.transitionTo(job, None, event.taskName, ExecState.Running)
        }
    })
  }

  @Subscribe
  def onProcessCompleted(event: ProcessCompletedEvent): Unit = {
    Await.result(storage.jobs.get(event.jobName).flatMap {
      case None =>
        logger.warn(s"Completed process associated with unknown job ${event.jobName}")
        Future.Done
      case Some(job) =>
        val nextState = if (event.exitCode == 0) ExecState.Successful else ExecState.Failed
        val result = TaskResult(event.exitCode, event.metrics, event.artifacts)
        job.parent match {
          case Some(parentName) =>
            storage.jobs.get(parentName).flatMap { parent =>
              stateManager.transitionTo(job, parent, event.taskName, nextState, result)
            }
          case None => stateManager.transitionTo(job, None, event.taskName, nextState, result)
        }
    })
  }
}