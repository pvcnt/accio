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

package fr.cnrs.liris.accio.framework.scheduler.standalone

import java.util.concurrent.ConcurrentLinkedQueue

import com.google.inject.{Inject, Singleton}
import com.twitter.util.{Await, Return, Throw}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent.{AssignTaskRequest, KillTaskRequest}
import fr.cnrs.liris.accio.framework.api.thrift._
import fr.cnrs.liris.accio.framework.scheduler._

import scala.collection.JavaConverters._

@Singleton
class StandaloneScheduler @Inject()(
  taskAssigner: TaskAssigner,
  clusterState: ClusterState,
  clientProvider: WorkerClientProvider)
  extends Scheduler with StrictLogging {

  private[this] val waitingTasks = new ConcurrentLinkedQueue[Task]

  override def submit(task: Task): Unit = {
    if (!maybeAssign(task)) {
      waitingTasks.add(task)
      logger.debug(s"Queued task ${task.id.value}")
    }
  }

  override def houseKeeping(kind: EventType): Unit = synchronized {
    kind match {
      case EventType.MoreResource =>
        // This state change could have freed some resources. We take the opportunity to assign new tasks.
        val it = waitingTasks.iterator
        it.asScala.foreach { task =>
          if (maybeAssign(task)) {
            it.remove()
          }
        }
      case _ => // Do nothing.
    }
  }

  override def kill(taskId: TaskId): Unit = {
    val maybeWorker = clusterState.read(_.find(_.activeTasks.exists(_.id == taskId)))
    maybeWorker match {
      case None =>
        logger.warn(s"No worker is currently executing ${taskId.value}")
        throw InvalidTaskException(taskId, Some("No worker is currently executing this task"))
      case Some(worker) => kill(worker, taskId)
    }
  }

  override def kill(runId: RunId): Set[Task] = {
    val activeWorkers = clusterState.read(_.filter(_.activeTasks.exists(task => task.runId == runId)))
    activeWorkers.flatMap { worker =>
      val tasks = worker.activeTasks.filter(task => task.runId == runId)
      tasks.foreach(task => kill(worker, task.id))
      tasks
    }
  }

  private def kill(worker: WorkerInfo, taskId: TaskId): Unit = {
    Await.result(clientProvider.apply(worker.dest).killTask(KillTaskRequest(taskId)))
    clusterState.update(worker.id, taskId, NodeStatus.Killed)
  }

  private def maybeAssign(task: Task): Boolean = synchronized {
    val maybeWorkerId = clusterState.read(taskAssigner.assign(task, _))
    maybeWorkerId match {
      case None => false
      case Some(worker) =>
        clusterState.assign(worker.id, task)
        val f = clientProvider.apply(worker.dest).assignTask(AssignTaskRequest(task)).liftToTry
        Await.result(f) match {
          case Return(_) => true
          case Throw(_: InvalidTaskException) =>
            // This error case should not happen, as it corresponds to the worker already handling the task under
            // scrutiny. If it happens, we log it but can let the execution continue safely.
            logger.error(s"Task ${task.id.value} is already assigned to worker ${worker.id.value}")
            true
          case Throw(_) =>
            // This is another error, undeclared by the worker Thrift service, likely to be a communication error.
            logger.error(s"Error while assigning task ${task.id.value} to worker ${worker.id.value}")
            false
        }
    }
  }
}