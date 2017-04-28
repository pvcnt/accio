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

import java.util.concurrent.ConcurrentHashMap
import javax.annotation.concurrent.ThreadSafe

import com.google.inject.Singleton
import com.twitter.util.Time
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.framework.api.thrift.TaskState.EnumUnknownTaskState
import fr.cnrs.liris.accio.framework.api.thrift._
import fr.cnrs.liris.accio.framework.util.Lockable

import scala.collection.JavaConverters._

/**
 * Hold information about the global state of a cluster. It handles registration and un-registration of workers
 * within the cluster, as well as assignments of tasks to those workers.
 *
 * Everything is stored in memory, and this class has to be a singleton.
 */
@ThreadSafe @Singleton
class ClusterState extends StrictLogging with Lockable[String] {
  private[this] val index = new ConcurrentHashMap[String, WorkerInfo]().asScala

  /**
   * Register a worker as part of this cluster.
   *
   * @param workerId  Worker identifier, unique among all workers.
   * @param dest      Destination where to reach this worker (Finagle name).
   * @param resources Total resources available to be scheduled on this worker.
   * @throws InvalidWorkerException If the worker is already registered.
   */
  @throws[InvalidWorkerException]
  def register(workerId: WorkerId, dest: String, resources: Resource): Unit = locked(workerId.value) {
    if (index.contains(workerId.value)) {
      throw InvalidWorkerException(workerId, Some("Worker is already registered"))
    }
    index(workerId.value.intern()) = WorkerInfo(workerId, dest, resources)
  }

  /**
   * Un-register a worker from this cluster. All subsequent requests concerning this worker will be rejected.
   *
   * @param workerId Worker identifier.
   * @throws InvalidWorkerException If the worker is not registered.
   */
  @throws[InvalidWorkerException]
  def unregister(workerId: WorkerId): Unit = locked(workerId.value) {
    if (index.remove(workerId.value).isEmpty) {
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
  def recordHeartbeat(workerId: WorkerId, at: Time = Time.now): Unit = locked(workerId.value) {
    val worker = apply(workerId)
    index(workerId.value.intern()) = worker.copy(heartbeatAt = at)
  }

  /**
   * Assign a task to be processed by a specific worker.
   *
   * @param workerId Worker identifier.
   * @param task     Task that will be processed.
   * @throws InvalidWorkerException If the worker is not registered of if the task is already assigned to another worker.
   */
  @throws[InvalidWorkerException]
  def assign(workerId: WorkerId, task: Task): Unit = locked(workerId.value) {
    index.values.find(_.activeTasks.exists(_.id == task.id)) match {
      case Some(w) =>
        logger.debug(s"Task ${task.id.value} is already assigned to worker ${w.id.value}")
        if (w.id != workerId) {
          throw InvalidWorkerException(workerId, Some(s"Task ${task.id.value} is already assigned to worker ${w.id.value}"))
        }
      case None =>
        var worker = apply(workerId)
        // Add starting task to running tasks and update resources of the worker accordingly.
        val runningTasks = worker.activeTasks + task.copy(status = TaskState.Scheduled)
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

        index(workerId.value.intern()) = worker
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
   * @throws InvalidWorkerException If the worker is not registered of if the task is not assigned to this worker.
   */
  @throws[InvalidWorkerException]
  def update(workerId: WorkerId, taskId: TaskId, status: TaskState): Unit = locked(workerId.value) {
    // Remove completed task from running tasks and update resources of the worker accordingly.
    var worker = ensure(workerId, taskId)
    val task = worker.activeTasks.find(_.id == taskId).get
    status match {
      case TaskState.Waiting | TaskState.Scheduled =>
        throw new IllegalArgumentException(s"Cannot update task ${taskId.value}: ${task.status} => $status")
      case TaskState.Running =>
        worker = worker.copy(activeTasks = worker.activeTasks.filter(_.id != taskId) ++ Seq(task.copy(status = status)))
        logger.debug(s"Updated ${task.id.value}: ${task.status} => $status")
      case TaskState.Success | TaskState.Failed | TaskState.Killed | TaskState.Cancelled | TaskState.Lost =>
        //TODO: check transition is correct. But how to mark resources as freed in case of an incorrect transition??
        if (task.status != TaskState.Running && status != TaskState.Lost) {
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
          case TaskState.Success | TaskState.Failed =>
            worker = worker.copy(completedTasks = worker.completedTasks + 1)
          case TaskState.Lost =>
            worker = worker.copy(lostTasks = worker.lostTasks + 1)
          case _ => // Do nothing.
        }
        logger.debug(s"Un-assigned ${status.name} task ${task.id.value} from worker ${workerId.value}")
      case EnumUnknownTaskState(_) => throw new IllegalArgumentException
    }
    index(workerId.value.intern()) = worker
  }

  /**
   * Return workers that did not send a heartbeat recently.
   *
   * @param deadline Deadline for sending a heartbeat.
   */
  def lostWorkers(deadline: Time): Set[WorkerInfo] = index.values.toSet.filter(_.heartbeatAt < deadline)

  /**
   * Return a snapshot of current cluster state. Returned value is a copy of current workers in the cluster.
   */
  def snapshot: Set[WorkerInfo] = index.values.toSet

  @throws[InvalidWorkerException]
  def apply(workerId: WorkerId): WorkerInfo =
    index.get(workerId.value) match {
      case None => throw InvalidWorkerException(workerId, Some("Worker is not registered"))
      case Some(worker) => worker
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