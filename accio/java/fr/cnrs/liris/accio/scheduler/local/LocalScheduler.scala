/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.scheduler.local

import java.io.FileInputStream
import java.nio.file.{Files, Path}
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedDeque, Executors}

import com.google.common.eventbus.EventBus
import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.logging.Logging
import com.twitter.util.{Future, FuturePool}
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.api.{TaskCompletedEvent, TaskStartedEvent}
import fr.cnrs.liris.accio.scheduler.Scheduler
import fr.cnrs.liris.common.scrooge.BinaryScroogeSerializer
import fr.cnrs.liris.common.util.Platform

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.io.Source

final class LocalScheduler(
  statsReceiver: StatsReceiver,
  eventBus: EventBus,
  reservedResources: Resource,
  executorUri: String,
  executorArgs: Seq[String],
  forceScheduling: Boolean,
  dataDir: Path)
  extends Scheduler with Logging {

  private case class Running(task: Task, future: Future[_])

  private[this] val pending = new ConcurrentLinkedDeque[Task]
  private[this] val running = new ConcurrentHashMap[String, Running].asScala
  private[this] val javaBinary = {
    sys.env.get("JAVA_HOME").map(home => s"$home/bin/java").getOrElse("/usr/bin/java")
  }
  private[this] val pool = {
    val executor = Executors.newCachedThreadPool(new NamedPoolThreadFactory("scheduler"))
    FuturePool.interruptible(executor)
  }
  private[this] val totalResources = {
    Resource(
      cpu = sys.runtime.availableProcessors - reservedResources.cpu,
      ramMb = Platform.totalMemory.map(ram => ram.inMegabytes - reservedResources.ramMb).getOrElse(0),
      diskMb = Platform.totalDiskSpace.map(disk => disk.inMegabytes - reservedResources.diskMb).getOrElse(0))
  }
  // Although the reserve/release code is synchronized, we register available resources as an
  // AtomicReference because it is accessed by the gauges code, which can occur at any time.
  private[this] val availableResources = new AtomicReference(totalResources)

  // Register gauges.
  // Total resources are registered as gauges, even though values are not expected to vary.
  statsReceiver.provideGauge("scheduler", "total", "cpu")(totalResources.cpu.toFloat)
  statsReceiver.provideGauge("scheduler", "total", "ram_mb")(totalResources.ramMb.toFloat)
  statsReceiver.provideGauge("scheduler", "total", "disk_mb")(totalResources.diskMb.toFloat)

  statsReceiver.provideGauge("scheduler", "free", "cpu")(availableResources.get.cpu.toFloat)
  statsReceiver.provideGauge("scheduler", "free", "ram_mb")(availableResources.get.ramMb.toFloat)
  statsReceiver.provideGauge("scheduler", "free", "disk_mb")(availableResources.get.diskMb.toFloat)

  statsReceiver.provideGauge("scheduler", "pending")(pending.size.toFloat)
  statsReceiver.provideGauge("scheduler", "running")(running.size.toFloat)

  override def submit(task: Task): Unit = {
    if (reserveResources(task.id, task.resource)) {
      val future = schedule(task)
      running(task.id) = Running(task, future)
    } else {
      if (!forceScheduling && !isEnoughResources(task.id, task.resource, totalResources)) {
        // TODO: We should cancel the task.
        logger.warn(s"Not enough resources to schedule task ${task.id}")
      }
      logger.info(s"Queued task ${task.id}")
      pending.add(task)
    }
  }

  override def kill(id: String): Boolean = {
    running.remove(id) match {
      case None => false
      case Some(item) =>
        item.future.raise(new RuntimeException)
        releaseResources(item.task.resource)
        true
    }
  }

  override def getLogs(id: String, kind: String, skip: Option[Int], tail: Option[Int]): Seq[String] = {
    readLogFile(getWorkDir(id).resolve(kind), skip, tail)
  }

  private def schedule(task: Task): Future[OpResult] = {
    logger.info(s"Starting execution of task ${task.id}")
    pool(execute(task))
      .onSuccess { result =>
        eventBus.post(TaskCompletedEvent(task.runId, task.nodeName, result, task.payload.cacheKey))
      }
      .onFailure { e =>
        logger.error(s"Unexpected error while executing task ${task.id}", e)
        eventBus.post(TaskCompletedEvent(task.runId, task.nodeName, OpResult(-998), task.payload.cacheKey))
      }
      .ensure {
        running.remove(task.id)
        releaseResources(task.resource)
      }
  }

  private def isEnoughResources(id: String, requests: Resource, resources: Resource): Boolean = {
    val ok = (requests.cpu == 0 || requests.cpu <= resources.cpu) &&
      (requests.ramMb == 0 || requests.ramMb <= resources.ramMb) &&
      (requests.diskMb == 0 || requests.diskMb <= resources.diskMb)
    if (ok) {
      true
    } else if (running.isEmpty && forceScheduling) {
      logger.warn(s"Forcing scheduling of task $id")
      true
    } else {
      false
    }
  }

  private def reserveResources(id: String, requests: Resource): Boolean = synchronized {
    val available = availableResources.get
    if (!isEnoughResources(id, requests, available)) {
      false
    } else {
      availableResources.set(Resource(
        cpu = available.cpu - requests.cpu,
        ramMb = available.ramMb - requests.ramMb,
        diskMb = available.diskMb - requests.diskMb))
      true
    }
  }

  private def releaseResources(requests: Resource): Unit = synchronized {
    val available = availableResources.get
    availableResources.set(Resource(
      cpu = available.cpu + requests.cpu,
      ramMb = available.ramMb + requests.ramMb,
      diskMb = available.diskMb + requests.diskMb))

    pending.asScala.foreach { task =>
      if (reserveResources(task.id, task.resource)) {
        pending.remove(task)
        val future = schedule(task)
        running(task.id) = Running(task, future)
      }
    }
  }

  private def execute(task: Task): OpResult = {
    val process = startProcess(task)
    eventBus.post(TaskStartedEvent(task.runId, task.nodeName))
    val exitCode = process.waitFor()
    logger.info(s"Completed execution of task ${task.id} (exit code: $exitCode)")
    read(getResultFile(getWorkDir(task.id))).copy(exitCode = exitCode)
  }

  private def startProcess(task: Task): Process = {
    val workDir = getWorkDir(task.id)
    Files.createDirectories(workDir)

    val outputsDir = getOutputsDir(workDir)
    Files.createDirectories(outputsDir)
    val command = createCommandLine(task, getResultFile(workDir))
    logger.debug(s"Command-line for task ${task.id}: ${command.mkString(" ")}")

    new ProcessBuilder()
      .command(command: _*)
      .directory(outputsDir.toFile)
      .redirectOutput(workDir.resolve("stdout").toFile)
      .redirectError(workDir.resolve("stderr").toFile)
      .start()
  }

  private def getOutputsDir(workDir: Path) = workDir.resolve("outputs")

  private def getResultFile(workDir: Path) = workDir.resolve("result")

  private def getWorkDir(taskId: String): Path = {
    dataDir
      .resolve(taskId(0).toString)
      .resolve(taskId(1).toString)
      .resolve(taskId)
  }

  private def readLogFile(file: Path, skip: Option[Int], tail: Option[Int]): Seq[String] = {
    var lines = Source.fromFile(file.toFile).getLines().toList
    skip.foreach(n => lines = lines.drop(n))
    tail.foreach(n => lines = lines.takeRight(n))
    lines
  }

  private def createCommandLine(task: Task, outputFile: Path): Seq[String] = {
    val cmd = mutable.ListBuffer.empty[String]
    cmd += javaBinary
    cmd ++= Seq("-cp", executorUri)
    cmd += s"-Xmx${task.resource.ramMb}M"
    cmd += "fr.cnrs.liris.accio.executor.AccioExecutorMain"
    cmd += BinaryScroogeSerializer.toString(task)
    cmd += outputFile.toAbsolutePath.toString
    cmd ++= executorArgs
    cmd.toList
  }

  private def read(file: Path): OpResult = {
    if (!file.toFile.canRead) {
      logger.warn(s"Result file is not readable: $file")
      OpResult(-997)
    } else {
      val fis = new FileInputStream(file.toFile)
      try {
        BinaryScroogeSerializer.read(fis, OpResult)
      } catch {
        case e: Throwable =>
          logger.warn(s"Error while reading result file: $file", e)
          OpResult(-996)
      } finally {
        fis.close()
      }
    }
  }
}
