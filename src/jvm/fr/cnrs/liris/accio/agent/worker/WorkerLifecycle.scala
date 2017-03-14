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

package fr.cnrs.liris.accio.agent.worker

import java.util.concurrent.atomic.AtomicReference

import com.google.inject.{Inject, Singleton}
import com.twitter.util._
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.agent.config.AgentConfig
import fr.cnrs.liris.accio.core.domain.Resource
import fr.cnrs.liris.accio.core.util.{InfiniteLoopThreadLike, ThreadManager, WorkerPool}

/**
 * Handles the registration lifecycle of a worker with its master, and the heartbeat.
 *
 * @param config Agent configuration.
 * @param state  Worker state.
 * @param client Client for the master server.
 * @param pool   Pool of threads.
 */
@Singleton
final class WorkerLifecycle @Inject()(
  config: AgentConfig,
  state: WorkerState,
  client: AgentService$FinagleClient,
  @WorkerPool pool: FuturePool)
  extends StrictLogging {

  private[this] val threads = new ThreadManager(pool)
  private[this] val status = new AtomicReference[WorkerLifecycle.Status](WorkerLifecycle.Status.Unregistered)

  /**
   * Register this worker to its master, and start a heartbeat thread.
   *
   * @return A future completed once the registration is done.
   */
  def register(): Future[Unit] = {
    if (status.compareAndSet(WorkerLifecycle.Status.Unregistered, WorkerLifecycle.Status.Registering)) {
      val maxResources = Resource(
        state.totalCpu,
        state.totalRam.map(_.inMegabytes).getOrElse(0),
        state.totalDisk.map(_.inMegabytes).getOrElse(0))
      // Executors are launched on the same host, without isolation, we can use directly the bind address.
      val dest = s"inet!${config.bind}:${config.worker.get.rpcPort}"
      val req = RegisterWorkerRequest(state.workerId, dest, maxResources)

      client.registerWorker(req)
        .handle {
          // It was already registered, ignore this error (though it should not happen).
          case InvalidWorkerException() => RegisterWorkerResponse()
        }
        .onFailure { e =>
          // Other error, report it and let the future fail.
          logger.error("Error while registering worker", e)
        }
        .onSuccess { _ =>
          if (status.compareAndSet(WorkerLifecycle.Status.Registering, WorkerLifecycle.Status.Ready)) {
            threads.submit(new HeartbeatThread)
          }
        }
        .unit
    } else {
      Future.Done
    }
  }

  /**
   * Un-register this worker from its master.
   *
   * @return A future completed once the un-registration is done. It never results in a failure.
   */
  def unregister(): Future[Unit] = {
    if (status.compareAndSet(WorkerLifecycle.Status.Ready, WorkerLifecycle.Status.Unregistered)) {
      threads.killAll()
      client
        .unregisterWorker(UnregisterWorkerRequest(state.workerId))
        .handle {
          case InvalidWorkerException() =>
            // It was already un-registered, ignore this error (though it should not happen).
            UnregisterWorkerResponse()
          case e: Throwable =>
            // Other error, report it and ignore it. This worker will be detected as lost by the master.
            logger.error("Error while un-registering worker", e)
            UnregisterWorkerResponse()
        }
        .unit
    } else {
      Future.Done
    }
  }

  private class HeartbeatThread extends InfiniteLoopThreadLike {
    override def singleOperation(): Unit = {
      val f = client.heartbeatWorker(HeartbeatWorkerRequest(state.workerId)).liftToTry
      Await.result(f) match {
        case Return(_) => sleep(AgentConfig.WorkerTimeout / 2)
        case Throw(InvalidWorkerException()) => kill()
        case Throw(e) =>
          logger.error(s"Error while sending heartbeat", e)
          sleep(AgentConfig.WorkerTimeout / 4)
      }
    }
  }

}

private object WorkerLifecycle {

  sealed trait Status

  object Status {

    case object Unregistered extends Status

    case object Registering extends Status

    case object Ready extends Status

  }

}