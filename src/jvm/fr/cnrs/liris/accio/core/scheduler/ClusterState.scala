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

package fr.cnrs.liris.accio.core.scheduler

import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.concurrent.ThreadSafe

import com.google.inject.Singleton
import com.twitter.util.Time
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.api.NodeStatus.EnumUnknownNodeStatus
import fr.cnrs.liris.accio.core.api.{InvalidTaskException, _}

import scala.collection.mutable

/**
 * Hold information about the global state of a cluster. It handles registration and un-registration of workers
 * within the cluster, as well as assignments of tasks to those workers.
 *
 * Everything is stored in memory, and this class has to be a singleton.
 */
@ThreadSafe
@Singleton
class ClusterState extends StrictLogging {
  private[this] val index = mutable.Map.empty[WorkerId, WorkerInfo]
  private[this] val lock = new ReentrantReadWriteLock

  /**
   * Register a worker as part of this cluster.
   *
   * @param workerId  Worker identifier, unique among all workers.
   * @param dest      Destination where to reach this worker (Finagle name).
   * @param resources Total resources available to be scheduled on this worker.
   * @throws InvalidWorkerException If the worker is already registered.
   */
  @throws[InvalidWorkerException]
  def register(workerId: WorkerId, dest: String, resources: Resource): Unit = write {
    if (index.contains(workerId)) {
      logger.debug(s"Worker ${workerId.value} is already registered")
      throw InvalidWorkerException(workerId, Some("Worker is already registered"))
    }
    index(workerId) = WorkerInfo(workerId, dest, resources)
  }

  /**
   * Un-register a worker from this cluster. All subsequent requests concerning this worker will be rejected.
   *
   * @param workerId Worker identifier.
   * @throws InvalidWorkerException If the worker is not registered.
   */
  @throws[InvalidWorkerException]
  def unregister(workerId: WorkerId): Unit = write {
    if (index.remove(workerId).isEmpty) {
      logger.debug(s"Worker ${workerId.value} is not registered")
      throw InvalidWorkerException(workerId, Some("Worker is not registered"))
    }
  }

  /**
   * Record a heartbeat from a worker.
   *
   * @param workerId Worker identifier.
   * @param at       Time at which the heartbeat is recorded.
   * @throws InvalidWorkerException If the worker is not registered.
   */
  @throws[InvalidWorkerException]
  def recordHeartbeat(workerId: WorkerId, at: Time = Time.now): Unit = write {
    val worker = apply(workerId)
    index(workerId) = worker.copy(heartbeatAt = at)
  }

  /**
   * Assign a task to be processed by a specific worker.
   *
   * @param workerId Worker identifier.
   * @param task     Task that will be processed.
   * @throws InvalidWorkerException If the worker is not registered.
   * @throws InvalidTaskException   If the task is already assigned to another worker.
   */
  @throws[InvalidWorkerException]
  @throws[InvalidTaskException]
  def assign(workerId: WorkerId, task: Task): Unit = write {
    index.values.find(_.activeTasks.exists(_.id == task.id)) match {
      case Some(w) =>
        logger.debug(s"Task ${task.id.value} is already assigned to worker ${w.id.value}")
        if (w.id != workerId) {
          throw InvalidTaskException(task.id, Some(s"Task is already assigned to worker ${w.id.value}"))
        }
      case None =>
        var worker = apply(workerId)
        // Add starting task to running tasks and update resources of the worker accordingly.
        val runningTasks = worker.activeTasks + task.copy(status = NodeStatus.Scheduled)
        val availableResources = Resource(
          worker.availableResources.cpu - task.resource.cpu,
          worker.availableResources.ramMb - task.resource.ramMb,
          worker.availableResources.diskMb - task.resource.diskMb)
        val reservedResources = Resource(
          worker.reservedResources.cpu + task.resource.cpu,
          worker.reservedResources.ramMb + task.resource.ramMb,
          worker.reservedResources.diskMb + task.resource.diskMb)
        worker = worker.copy(
          activeTasks = runningTasks,
          availableResources = availableResources,
          reservedResources = reservedResources)

        index(workerId) = worker
        logger.debug(s"Assigned task ${task.id.value} to worker ${workerId.value}")
    }
  }

  /**
   * Ensure that a task is actually assigned to a specific worker. It is designed to be used as a consistency check,
   * guaranteeing that only valid requests from workers are processed.
   *
   * You do not have to call it before other methods of this class, as it is already integrated.
   *
   * @param workerId Worker identifier.
   * @param taskId   Task identifier.
   * @throws InvalidWorkerException If the task is not assigned to the previously specified worker.
   * @return Worker identity.
   */
  @throws[InvalidWorkerException]
  def ensure(workerId: WorkerId, taskId: TaskId): WorkerInfo = {
    val worker = apply(workerId)
    if (!worker.activeTasks.exists(_.id == taskId)) {
      logger.warn(s"Task ${taskId.value} is not registered with worker ${workerId.value}")
      throw new InvalidWorkerException(workerId, Some(s"Task ${taskId.value} is not registered with this worker"))
    }
    worker
  }

  /**
   * Un-assign a task from a worker.
   *
   * @param workerId Worker identifier.
   * @param taskId   Task identifier.
   * @param status   Final task status.
   * @throws InvalidWorkerException If the worker is not registered.
   * @throws InvalidTaskException   If the task is not assigned to the previously specified worker.
   */
  @throws[InvalidWorkerException]
  @throws[InvalidTaskException]
  def update(workerId: WorkerId, taskId: TaskId, status: NodeStatus): Unit = write {
    // Remove completed task from running tasks and update resources of the worker accordingly.
    var worker = ensure(workerId, taskId)
    val task = worker.activeTasks.find(_.id == taskId).get
    status match {
      case NodeStatus.Waiting | NodeStatus.Scheduled =>
        throw new IllegalArgumentException(s"Cannot update task ${taskId.value}: ${task.status} => $status")
      case NodeStatus.Running =>
        worker = worker.copy(activeTasks = worker.activeTasks.filter(_.id != taskId) ++ Seq(task.copy(status = status)))
        logger.debug(s"Updated ${task.id.value}: ${task.status} => $status")
      case NodeStatus.Success | NodeStatus.Failed | NodeStatus.Killed | NodeStatus.Cancelled | NodeStatus.Lost =>
        //TODO: check transition is correct. But how to mark resources as freed in case of an incorrect transition??
        if (task.status != NodeStatus.Running && status != NodeStatus.Lost) {
          logger.warn(s"Suspicious transition of task ${taskId.value}: ${task.status} => $status")
        }
        val runningTasks = worker.activeTasks.filterNot(_.id == taskId)
        val availableResources = Resource(
          worker.availableResources.cpu + task.resource.cpu,
          worker.availableResources.ramMb + task.resource.ramMb,
          worker.availableResources.diskMb + task.resource.diskMb)
        val reservedResources = Resource(
          worker.reservedResources.cpu - task.resource.cpu,
          worker.reservedResources.ramMb - task.resource.ramMb,
          worker.reservedResources.diskMb - task.resource.diskMb)
        worker = worker.copy(
          activeTasks = runningTasks,
          availableResources = availableResources,
          reservedResources = reservedResources)

        // Update worker stats depending on task state.
        status match {
          case NodeStatus.Success | NodeStatus.Failed =>
            worker = worker.copy(completedTasks = worker.completedTasks + 1)
          case NodeStatus.Lost =>
            worker = worker.copy(lostTasks = worker.lostTasks + 1)
          case _ => // Do nothing.
        }
        logger.debug(s"Un-assigned ${status.name} task ${task.id.value} from worker ${workerId.value}")
      case EnumUnknownNodeStatus(_) => throw new IllegalArgumentException
    }
    index(workerId) = worker
  }

  /**
   * Return workers that did not send a heartbeat recently.
   *
   * @param deadline Deadline for sending a heartbeat.
   */
  def lostWorkers(deadline: Time): Set[WorkerInfo] = read(_.filter(_.heartbeatAt < deadline))

  /**
   * Run an arbitrary section of code, accessing the inner cluster state (read-only).
   *
   * @param fn Function to execute.
   * @tparam T Return type.
   * @return Return value of `fn`.
   */
  def read[T](fn: Set[WorkerInfo] => T): T = {
    val lock = this.lock.readLock()
    lock.lock()
    try {
      fn(index.values.toSet)
    } finally {
      lock.unlock()
    }
  }

  private def write[T](fn: => T) = {
    val lock = this.lock.writeLock()
    lock.lock()
    try {
      fn
    } finally {
      lock.unlock()
    }
  }

  @throws[InvalidWorkerException]
  def apply(workerId: WorkerId): WorkerInfo = read { _ =>
    index.get(workerId) match {
      case None =>
        logger.debug(s"Worker ${workerId.value} is not registered")
        throw InvalidWorkerException(workerId, Some("Worker is not registered"))
      case Some(worker) => worker
    }
  }
}

/**
 * Hold information about a worker and its state.
 *
 * @param id                 Worker identifier, unique among all workers.
 * @param dest               Destination where to reach this worker (Finagle name).
 * @param registeredAt       Time at which this worker registered to the cluster.
 * @param heartbeatAt        Time at which this worker sent its last heartbeat.
 * @param maxResources       Total resources available to be scheduled.
 * @param availableResources Current resources available to be scheduled.
 * @param reservedResources  Current resources reserved by running tasks.
 * @param activeTasks        List of active tasks (scheduled or running).
 * @param completedTasks     Number of completed tasks (either successfully or not, but that went to completion).
 * @param lostTasks          Number of lost tasks.
 */
case class WorkerInfo private(
  id: WorkerId,
  dest: String,
  registeredAt: Time,
  heartbeatAt: Time,
  maxResources: Resource,
  availableResources: Resource,
  reservedResources: Resource,
  activeTasks: Set[Task],
  completedTasks: Int,
  lostTasks: Int)

/**
 * Factory for [[WorkerInfo]].
 */
private object WorkerInfo {
  def apply(id: WorkerId, dest: String, resources: Resource): WorkerInfo =
    WorkerInfo(id, dest, Time.now, Time.now, resources, resources, Resource(0, 0, 0), Set.empty, 0, 0)
}