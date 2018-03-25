/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.agent.handler

import com.google.inject.{Inject, Singleton}
import com.twitter.util.{Await, Duration, Future, Time}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent.config.ExecutorTimeout
import fr.cnrs.liris.accio.agent.{AgentService$FinagleClient, LostTaskRequest}
import fr.cnrs.liris.accio.api.thrift.{ExecutorId, TaskId}
import fr.cnrs.liris.accio.util.InfiniteLoopThreadLike

/**
 * Observer tracking lost executors.
 *
 * @param state  Worker state.
 * @param client Client for the master server.
 */
@Singleton
final class LostExecutorsObserver @Inject()(client: AgentService$FinagleClient, state: WorkerState, @ExecutorTimeout timeout: Duration)
  extends InfiniteLoopThreadLike with StrictLogging {

  override def singleOperation(): Unit = {
    val fs = state
      .lostExecutors(Time.now - timeout)
      .map { case (executorId, taskId) => handleLostExecutor(executorId, taskId) }
    Await.ready(Future.collect(fs.toSeq))
    sleep(timeout)
  }

  private def handleLostExecutor(executorId: ExecutorId, taskId: TaskId): Future[Unit] = {
    // Lost executors are locally unregistered and lost tasks reported to the master.
    logger.warn(s"Lost executor ${executorId.value} with task ${taskId.value}")
    state.unassign(executorId, taskId)
    client
      .lostTask(LostTaskRequest(state.workerId, taskId))
      .onFailure(e => logger.error(s"Error while reporting lost task ${taskId.value}", e))
      .unit
  }
}