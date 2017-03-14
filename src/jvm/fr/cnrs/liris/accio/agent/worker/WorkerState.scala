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

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.stats.{Gauge, StatsReceiver}
import com.twitter.util.{StorageUnit, Time}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent.InvalidExecutorException
import fr.cnrs.liris.accio.agent.config.AgentConfig
import fr.cnrs.liris.accio.core.domain.{ExecutorId, InvalidTaskException, TaskId, WorkerId}
import fr.cnrs.liris.common.util.Platform

import scala.collection.mutable

/**
 *
 * @param statsReceiver Stats receiver.
 */
@Singleton
class WorkerState @Inject()(config: AgentConfig, statsReceiver: StatsReceiver) extends StrictLogging {
  private[this] val pendingTasks = mutable.Set.empty[TaskId]
  private[this] val runningTasks = mutable.Map.empty[ExecutorId, TaskId]
  private[this] val heartbeats = mutable.Map.empty[ExecutorId, Time]
  private[this] val completedTaskCounter = statsReceiver.counter("task", "completed")

  val workerId = WorkerId(config.name)

  val totalCpu: Int = sys.runtime.availableProcessors - Math.ceil(config.worker.get.reserved.cpu).toInt
  val totalRam: Option[StorageUnit] = Platform.totalMemory.map(ram => ram - StorageUnit.fromMegabytes(config.worker.get.reserved.ramMb))
  val totalDisk: Option[StorageUnit] = Platform.totalDiskSpace.map(disk => disk - StorageUnit.fromMegabytes(config.worker.get.reserved.diskMb))

  logger.debug(s"Detected available resources: CPU $totalCpu, RAM ${totalRam.map(_.toHuman).getOrElse("<unknown>")}, Disk ${totalDisk.map(_.toHuman).getOrElse("<unknown>")}")

  // Set used for keeping track of gauges (otherwise only weakly referenced). We also need this class to be a
  // @Singleton, to prevent it from being GC'ed.
  private[this] val gauges = createGauges()

  @throws[InvalidTaskException]
  def register(taskId: TaskId): Unit = synchronized {
    if (pendingTasks.contains(taskId)) {
      logger.debug(s"Task ${taskId.value} is already registered")
      throw new InvalidTaskException
    } else {
      runningTasks.find(_._2 == taskId) match {
        case Some((existingExecutorId, _)) =>
          logger.debug(s"Task ${taskId.value} is already registered to executor ${existingExecutorId.value}")
          throw new InvalidTaskException
        case None =>
          pendingTasks += taskId
          logger.debug(s"Registered task ${taskId.value}")
      }
    }
  }

  @throws[InvalidTaskException]
  @throws[InvalidExecutorException]
  def assign(taskId: TaskId, executorId: ExecutorId): Unit = synchronized {
    get(taskId) match {
      case Some(existingExecutorId) =>
        logger.debug(s"Task ${taskId.value} is already registered to executor ${existingExecutorId.value}")
        if (existingExecutorId != executorId) {
          throw new InvalidTaskException
        }
      case None =>
        runningTasks.get(executorId) match {
          case Some(existingTaskId) =>
            // Case of existingTaskId == taskId has already been handled above.
            logger.debug(s"Executor ${executorId.value} is already registered with task ${existingTaskId.value}")
            throw new InvalidExecutorException
          case None =>
            if (!pendingTasks.contains(taskId)) {
              logger.debug(s"Task ${taskId.value} is not registered")
              throw new InvalidTaskException
            }
            pendingTasks -= taskId
            runningTasks(executorId) = taskId
            heartbeats(executorId) = Time.now
            logger.debug(s"Registered executor ${executorId.value} with task ${taskId.value}")
        }
    }
  }

  @throws[InvalidTaskException]
  @throws[InvalidExecutorException]
  def ensure(taskId: TaskId, executorId: ExecutorId): Unit = synchronized {
    runningTasks.get(executorId) match {
      case None =>
        logger.debug(s"Executor ${executorId.value} is not registered")
        throw new InvalidExecutorException
      case Some(existingTaskId) =>
        if (existingTaskId != taskId) {
          throw new InvalidTaskException
        }
    }
  }

  @throws[InvalidExecutorException]
  def recordHeartbeat(executorId: ExecutorId): Unit = synchronized {
    heartbeats.get(executorId) match {
      case None =>
        logger.debug(s"Executor ${executorId.value} is not registered")
        throw new InvalidExecutorException
      case Some(_) => heartbeats(executorId) = Time.now
    }
  }

  def lostExecutors(deadline: Time): Set[(ExecutorId, TaskId)] = synchronized {
    heartbeats
      .filter(_._2 < deadline)
      .keySet
      .map(executorId => executorId -> runningTasks(executorId))
      .toSet // Scala collections is sometimes a mess...
  }

  @throws[InvalidTaskException]
  @throws[InvalidExecutorException]
  def unassign(executorId: ExecutorId, taskId: TaskId): Unit = synchronized {
    ensure(taskId, executorId)
    runningTasks -= executorId
    heartbeats -= executorId
    completedTaskCounter.incr()
    logger.debug(s"Un-registered executor ${executorId.value} from task ${taskId.value}")
  }

  @throws[InvalidTaskException]
  def unregister(taskId: TaskId): Unit = synchronized {
    get(taskId) match {
      case Some(existingExecutorId) =>
        runningTasks.remove(existingExecutorId)
        logger.debug(s"Un-registered task ${taskId.value} from executor ${existingExecutorId.value}")
      case None =>
        if (!pendingTasks.contains(taskId)) {
          logger.debug(s"Task ${taskId.value} is not registered")
          throw new InvalidTaskException
        }
        pendingTasks -= taskId
        logger.debug(s"Un-registered task ${taskId.value}")
    }
  }

  private def get(taskId: TaskId): Option[ExecutorId] = runningTasks.find(_._2 == taskId).map(_._1)

  /**
   * Return total number of CPUs reserved by running tasks.
   */
  //private def reservedCpu = monitors.values.map(_.task.resource.cpu).sum

  /**
   * Return total RAM reserved by running tasks.
   */
  //private def reservedRam = StorageUnit.fromMegabytes(monitors.values.map(_.task.resource.ramMb).sum)

  /**
   * Return total disk space reserved by running tasks.
   */
  //private def reservedDisk = StorageUnit.fromMegabytes(monitors.values.map(_.task.resource.diskMb).sum)

  private def createGauges(): Set[Gauge] = {
    val stats = statsReceiver.scope("accio", "worker")
    val gauges = mutable.Set.empty[Gauge]
    gauges += stats.addGauge("cpu", "max")(totalCpu)
    totalRam.foreach {
      ram =>
        gauges += stats.addGauge("ram", "max")(ram.inBytes)
    }
    totalDisk.foreach {
      disk =>
        gauges += stats.addGauge("disk", "max")(disk.inBytes)
    }
    /*gauges += stats.addGauge("cpu", "available")(workerState.totalCpu - reservedCpu.toFloat)
    workerState.totalRam.foreach { ram =>
      gauges += stats.addGauge("ram", "available")((ram - reservedRam).inBytes)
    }
    workerState.totalDisk.foreach { disk =>
      gauges += stats.addGauge("disk", "available")((disk - reservedDisk).inBytes)
    }
    gauges += stats.addGauge("cpu", "reserved")(reservedCpu.toFloat)
    gauges += stats.addGauge("ram", "reserved")(reservedRam.inBytes)
    gauges += stats.addGauge("disk", "reserved")(reservedDisk.inBytes)*/
    gauges += stats.addGauge("task", "pending")(pendingTasks.size)
    gauges += stats.addGauge("task", "running")(runningTasks.size)
    gauges.toSet
  }
}