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

import java.io.{ByteArrayOutputStream, FileInputStream}
import java.nio.file.{Files, Path}
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedDeque, Executors}

import com.google.common.eventbus.EventBus
import com.google.inject.{Inject, Singleton}
import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.Logging
import com.twitter.util.{Base64StringEncoder, Future, FuturePool}
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.api.{TaskCompletedEvent, TaskStartedEvent}
import fr.cnrs.liris.accio.config._
import fr.cnrs.liris.accio.scheduler.Scheduler
import fr.cnrs.liris.common.util.Platform
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TIOStreamTransport

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.io.Source

@Singleton
final class LocalScheduler @Inject()(
  statsReceiver: StatsReceiver,
  eventBus: EventBus,
  @ReservedResource reservedResources: Resource,
  @ExecutorUri executorUri: String,
  @ExecutorArgs executorArgs: Seq[String],
  @ForceScheduling forceScheduling: Boolean,
  @DataDir dataDir: Path)
  extends Scheduler with Logging {

  private case class Running(task: Task, future: Future[_])

  private[this] val protocolFactory = new TBinaryProtocol.Factory
  private[this] val pending = new ConcurrentLinkedDeque[Task]
  private[this] val running = new ConcurrentHashMap[TaskId, Running].asScala
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
  private[this] var availableResources = totalResources

  // Register gauges.
  /*totalResources.foreach { case (key, value) =>
    // Total resources are registered as gauges, even though values are not expected to vary.
    statsReceiver.provideGauge("scheduler", "total", key)(value.toFloat)
  }
  availableResources.keySet.foreach { key =>
    statsReceiver.provideGauge("scheduler", "available", key)(availableResources(key).toFloat)
  }*/
  statsReceiver.provideGauge("scheduler", "pending")(pending.size.toFloat)
  statsReceiver.provideGauge("scheduler", "running")(running.size.toFloat)


  override def submit(task: Task): Unit = {
    if (reserveResources(task.id, task.resource)) {
      val future = schedule(task)
      running(task.id) = Running(task, future)
    } else {
      if (!forceScheduling && !isEnoughResources(task.id, task.resource, totalResources)) {
        // TODO: We should cancel the task.
        logger.warn(s"Not enough resources to schedule task ${task.id.value}")
      }
      logger.info(s"Queued task ${task.id.value}")
      pending.add(task)
    }
  }

  override def kill(id: TaskId): Boolean = {
    running.remove(id) match {
      case None => false
      case Some(item) =>
        item.future.raise(new RuntimeException)
        releaseResources(item.task.resource)
        true
    }
  }

  override def kill(id: RunId): Set[Task] = {
    val tasks = mutable.Set.empty[Task]
    val pendingTasks = pending.asScala.filter(_.runId == id)
    tasks ++= pendingTasks.flatMap(task => if (pending.remove(task)) Some(task) else None)
    val runningTasks = running.values.filter(_.task.runId == id).map(_.task)
    tasks ++= runningTasks.flatMap(task => if (kill(task.id)) Some(task) else None)
    tasks.toSet
  }

  override def getLogs(id: TaskId, kind: String, skip: Option[Int], tail: Option[Int]): Seq[String] = {
    readLogFile(getWorkDir(id).resolve(kind), skip, tail)
  }

  private def schedule(task: Task): Future[OpResult] = {
    logger.info(s"Starting execution of task ${task.id.value}")
    pool(execute(task))
      .onSuccess { result =>
        eventBus.post(TaskCompletedEvent(task.runId, task.nodeName, result, task.payload.cacheKey))
      }
      .onFailure { e =>
        logger.error(s"Unexpected error while executing task ${task.id.value}", e)
        eventBus.post(TaskCompletedEvent(task.runId, task.nodeName, OpResult(-998), task.payload.cacheKey))
      }
      .ensure {
        running.remove(task.id)
        releaseResources(task.resource)
      }
  }

  private def isEnoughResources(id: TaskId, requests: Resource, resources: Resource): Boolean = {
    val ok = requests.cpu <= resources.cpu &&
      requests.ramMb <= resources.ramMb &&
      requests.diskMb <= resources.diskMb
    if (ok) {
      true
    } else if (running.isEmpty && forceScheduling) {
      logger.warn(s"Forcing scheduling of task ${id.value}")
      true
    } else {
      false
    }
  }

  private def reserveResources(id: TaskId, requests: Resource): Boolean = synchronized {
    if (!isEnoughResources(id, requests, availableResources)) {
      false
    } else {
      availableResources = Resource(
        cpu = availableResources.cpu - requests.cpu,
        ramMb = availableResources.ramMb - requests.ramMb,
        diskMb = availableResources.diskMb - requests.diskMb)
      true
    }
  }

  private def releaseResources(requests: Resource): Unit = synchronized {
    availableResources = Resource(
      cpu = availableResources.cpu + requests.cpu,
      ramMb = availableResources.ramMb + requests.ramMb,
      diskMb = availableResources.diskMb + requests.diskMb)

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
    logger.info(s"Completed execution of task ${task.id.value} (exit code: $exitCode)")
    read(getResultFile(getWorkDir(task.id))).copy(exitCode = exitCode)
  }

  private def startProcess(task: Task): Process = {
    val workDir = getWorkDir(task.id)
    Files.createDirectories(workDir)

    val outputsDir = getOutputsDir(workDir)
    Files.createDirectories(outputsDir)
    val command = createCommandLine(task, getResultFile(workDir))
    logger.debug(s"Command-line for task ${task.id.value}: ${command.mkString(" ")}")

    new ProcessBuilder()
      .command(command: _*)
      .directory(outputsDir.toFile)
      .redirectOutput(workDir.resolve("stdout").toFile)
      .redirectError(workDir.resolve("stderr").toFile)
      .start()
  }

  private def getOutputsDir(workDir: Path) = workDir.resolve("outputs")

  private def getResultFile(workDir: Path) = workDir.resolve("result")

  private def getWorkDir(taskId: TaskId): Path = {
    dataDir
      .resolve(taskId.value(0).toString)
      .resolve(taskId.value(1).toString)
      .resolve(taskId.value)
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
    cmd += encode(task)
    cmd += outputFile.toAbsolutePath.toString
    cmd ++= executorArgs
    cmd.toList
  }

  private def encode(task: Task): String = {
    val baos = new ByteArrayOutputStream
    val protocol = protocolFactory.getProtocol(new TIOStreamTransport(baos))
    task.write(protocol)
    Base64StringEncoder.encode(baos.toByteArray)
  }

  private def read(file: Path): OpResult = {
    if (!file.toFile.canRead) {
      logger.warn(s"Result file is not readable: $file")
      OpResult(-997)
    } else {
      val fis = new FileInputStream(file.toFile)
      val protocol = protocolFactory.getProtocol(new TIOStreamTransport(fis))
      try {
        OpResult.decode(protocol)
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
