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
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedDeque, Executors}

import com.google.common.eventbus.EventBus
import com.google.inject.{Inject, Singleton}
import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.Logging
import com.twitter.util.{Base64StringEncoder, Future, FuturePool}
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.api.{TaskCompletedEvent, TaskStartedEvent}
import fr.cnrs.liris.accio.config.{DataDir, ExecutorArgs, ExecutorUri, ReservedResource}
import fr.cnrs.liris.accio.scheduler.Scheduler
import fr.cnrs.liris.common.util.Platform
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TIOStreamTransport

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.io.Source

// TODO: check for concurrency-correctness.
@Singleton
final class LocalScheduler @Inject()(
  statsReceiver: StatsReceiver,
  eventBus: EventBus,
  @ReservedResource reservedResources: Resource,
  @ExecutorUri executorUri: String,
  @ExecutorArgs executorArgs: Seq[String],
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
  private[this] val resourceLock = new ReentrantLock

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
    if (reserveResources(task.resource)) {
      logger.info(s"Starting execution of task ${task.id.value}")
      val future = schedule(task)
      running(task.id) = Running(task, future)
    } else {
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

  def getLogs(id: TaskId, kind: String, tail: Option[Int] = None): Seq[String] = {
    readLogFile(getWorkDir(id).resolve(kind), tail)
  }

  private def trySchedule(): Unit = synchronized {
    pending.asScala.foreach { task =>
      if (reserveResources(task.resource)) {
        pending.remove(task)
        logger.info(s"Starting execution of task ${task.id.value}")
        val future = schedule(task)
        running(task.id) = Running(task, future)
      }
    }
  }

  private def schedule(task: Task): Future[OpResult] = {
    pool(execute(task))
      .onSuccess { result =>
        eventBus.post(TaskCompletedEvent(task.runId, task.nodeName, result, task.payload.cacheKey))
      }
      .onFailure { e =>
        logger.error(s"Unexpected error while executing task ${task.id.value}", e)
        eventBus.post(TaskCompletedEvent(task.runId, task.nodeName, OpResult(-999), task.payload.cacheKey))
      }
      .ensure {
        running.remove(task.id)
        releaseResources(task.resource)
      }
  }

  private def isEnoughResources(requests: Resource, resources: Resource): Boolean = {
    requests.cpu <= resources.cpu &&
      requests.ramMb <= resources.ramMb &&
      requests.diskMb <= resources.diskMb
  }

  private def reserveResources(requests: Resource): Boolean = {
    resourceLock.lock()
    try {
      if (!isEnoughResources(requests, availableResources)) {
        false
      } else {
        availableResources = Resource(
          cpu = availableResources.cpu - requests.cpu,
          ramMb = availableResources.ramMb - requests.ramMb,
          diskMb = availableResources.diskMb - requests.diskMb)
        true
      }
    } finally {
      resourceLock.unlock()
    }
  }

  private def releaseResources(requests: Resource): Unit = {
    resourceLock.lock()
    try {
      availableResources = Resource(
        cpu = availableResources.cpu + requests.cpu,
        ramMb = availableResources.ramMb + requests.ramMb,
        diskMb = availableResources.diskMb + requests.diskMb)
    } finally {
      resourceLock.unlock()
    }
    trySchedule()
  }

  private def execute(task: Task): OpResult = {
    // TODO: stream logs.
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

  private def readLogFile(file: Path, tail: Option[Int]): Seq[String] = {
    val lines = Source.fromFile(file.toFile).getLines().toSeq
    tail match {
      case Some(n) if lines.size > n => lines.takeRight(n)
      case _ => lines
    }
  }

  private def createCommandLine(task: Task, outputFile: Path): Seq[String] = {
    val cmd = mutable.ListBuffer.empty[String]
    cmd += javaBinary
    cmd ++= Seq("-cp", executorUri)
    cmd += s"-Xmx${task.resource.ramMb}M"
    cmd += "fr.cnrs.liris.accio.executor.AccioExecutorMain"
    cmd ++= executorArgs
    cmd ++= Seq("-com.twitter.jvm.numProcs", task.resource.cpu.toString)
    cmd += encode(task)
    cmd += outputFile.toAbsolutePath.toString
    cmd.toList
  }

  private def encode(task: Task): String = {
    val baos = new ByteArrayOutputStream
    val protocol = protocolFactory.getProtocol(new TIOStreamTransport(baos))
    task.write(protocol)
    Base64StringEncoder.encode(baos.toByteArray)
  }

  private def read(file: Path): OpResult = {
    val fis = new FileInputStream(file.toFile)
    try {
      val protocol = protocolFactory.getProtocol(new TIOStreamTransport(fis))
      OpResult.decode(protocol)
    } finally {
      fis.close()
    }
  }
}
