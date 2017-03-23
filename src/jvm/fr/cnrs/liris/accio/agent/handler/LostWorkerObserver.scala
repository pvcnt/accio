/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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
import com.twitter.util.{Duration, Time}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent.LostTaskRequest
import fr.cnrs.liris.accio.agent.config.WorkerTimeout
import fr.cnrs.liris.accio.core.scheduler.{ClusterState, WorkerInfo}
import fr.cnrs.liris.accio.core.util.InfiniteLoopThreadLike

/**
 * Observer tracking lost tasks.
 */
@Singleton
final class LostWorkerObserver @Inject()(state: ClusterState, lostTaskHandler: LostTaskHandler, @WorkerTimeout timeout: Duration)
  extends InfiniteLoopThreadLike with StrictLogging {

  override def singleOperation(): Unit = {
    state
      .lostWorkers(Time.now - timeout)
      .foreach(handleLostWorker)
    sleep(timeout)
  }


  private def handleLostWorker(worker: WorkerInfo) = {
    logger.warn(s"Lost worker ${worker.id.value} (with ${worker.activeTasks.size} active tasks)")
    worker.activeTasks.foreach(task => lostTaskHandler.handle(LostTaskRequest(worker.id, task.id)))
    state.unregister(worker.id)
  }
}