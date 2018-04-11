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
import java.lang.{Process => JavaProcess}
import java.nio.file.{Files, Path}
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedDeque, Executors}

import com.google.common.eventbus.EventBus
import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.io.{Buf, Reader}
import com.twitter.util.logging.Logging
import com.twitter.util.{Future, FuturePool}
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.api.{ProcessCompletedEvent, ProcessStartedEvent}
import fr.cnrs.liris.accio.scheduler.{Process, Scheduler}
import fr.cnrs.liris.util.Platform
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

import scala.collection.JavaConverters._
import scala.collection.mutable

final class LocalScheduler(
  statsReceiver: StatsReceiver,
  eventBus: EventBus,
  reservedResources: ComputeResources,
  executorUri: String,
  executorArgs: Seq[String],
  forceScheduling: Boolean,
  dataDir: Path)
  extends Scheduler with Logging {

  private case class Running(process: Process, future: Future[_])

  private[this] val pending = new ConcurrentLinkedDeque[Process]
  private[this] val running = new ConcurrentHashMap[String, Running].asScala
  private[this] val javaBinary = {
    sys.env.get("JAVA_HOME").map(home => s"$home/bin/java").getOrElse("/usr/bin/java")
  }
  private[this] val pool = {
    val executor = Executors.newCachedThreadPool(new NamedPoolThreadFactory("scheduler"))
    FuturePool.interruptible(executor)
  }
  private[this] val totalResources = {
    ComputeResources(
      cpus = sys.runtime.availableProcessors - reservedResources.cpus,
      ramMb = Platform.totalMemory.map(ram => ram.inMegabytes - reservedResources.ramMb).getOrElse(0),
      diskGb = Platform.totalDiskSpace.map(disk => disk.inGigabytes - reservedResources.diskGb).getOrElse(0))
  }
  // Although the reserve/release code is synchronized, we register available resources as an
  // AtomicReference because it is accessed by the gauges code, which can occur at any time.
  private[this] val availableResources = new AtomicReference(totalResources)

  // Register gauges.
  // Total resources are registered as gauges, even though values are not expected to vary.
  statsReceiver.provideGauge("scheduler", "total", "cpus")(totalResources.cpus.toFloat)
  statsReceiver.provideGauge("scheduler", "total", "ram_mb")(totalResources.ramMb.toFloat)
  statsReceiver.provideGauge("scheduler", "total", "disk_gb")(totalResources.diskGb.toFloat)

  statsReceiver.provideGauge("scheduler", "free", "cpus")(availableResources.get.cpus.toFloat)
  statsReceiver.provideGauge("scheduler", "free", "ram_mb")(availableResources.get.ramMb.toFloat)
  statsReceiver.provideGauge("scheduler", "free", "disk_gb")(availableResources.get.diskGb.toFloat)

  statsReceiver.provideGauge("scheduler", "pending")(pending.size.toFloat)
  statsReceiver.provideGauge("scheduler", "running")(running.size.toFloat)

  override def submit(process: Process): Unit = {
    if (reserveResources(process.name, process.payload.resources)) {
      val future = schedule(process)
      running(process.name) = Running(process, future)
    } else {
      if (!forceScheduling && !isEnoughResources(process.name, process.payload.resources, totalResources)) {
        // TODO: We should cancel the task.
        logger.warn(s"Not enough resources to schedule task ${process.name} (${process.payload.resources})")
      }
      logger.info(s"Queued process ${process.name}")
      pending.add(process)
    }
  }

  override def kill(name: String): Future[Boolean] = {
    running.remove(name) match {
      case None => Future.value(false)
      case Some(item) =>
        item.future.raise(new RuntimeException)
        releaseResources(item.process.payload.resources)
        Future.value(true)
    }
  }

  override def getLogs(name: String, kind: String, skip: Option[Int], tail: Option[Int]): Future[Seq[String]] = {
    readLogFile(getWorkDir(name).resolve(kind), skip, tail)
  }

  private def schedule(process: Process): Future[Unit] = {
    pool(execute(process))
      .onSuccess { exitCode =>
        read(getResultFile(getWorkDir(process.name))) match {
          case Some(result) =>
            eventBus.post(ProcessCompletedEvent(process.jobName, process.taskName, exitCode, result.artifacts, result.metrics))
          case None =>
            eventBus.post(ProcessCompletedEvent(process.jobName, process.taskName, -1, Seq.empty, Seq.empty))
        }
      }
      .onFailure { e =>
        logger.error(s"Unexpected error while executing process ${process.name}", e)
        eventBus.post(ProcessCompletedEvent(process.jobName, process.taskName, -1, Seq.empty, Seq.empty))
      }
      .ensure {
        running.remove(process.name)
        releaseResources(process.payload.resources)
      }
      .unit
  }

  private def isEnoughResources(id: String, requests: ComputeResources, resources: ComputeResources): Boolean = {
    val ok = (requests.cpus == 0 || requests.cpus <= resources.cpus) &&
      (requests.ramMb == 0 || requests.ramMb <= resources.ramMb) &&
      (requests.diskGb == 0 || requests.diskGb <= resources.diskGb)
    if (ok) {
      true
    } else if (running.isEmpty && forceScheduling) {
      logger.warn(s"Forcing scheduling of task $id")
      true
    } else {
      false
    }
  }

  private def reserveResources(id: String, requests: ComputeResources): Boolean = synchronized {
    val available = availableResources.get
    if (!isEnoughResources(id, requests, available)) {
      false
    } else {
      availableResources.set(ComputeResources(
        cpus = available.cpus - requests.cpus,
        ramMb = available.ramMb - requests.ramMb,
        diskGb = available.diskGb - requests.diskGb))
      true
    }
  }

  private def releaseResources(requests: ComputeResources): Unit = synchronized {
    val available = availableResources.get
    availableResources.set(ComputeResources(
      cpus = available.cpus + requests.cpus,
      ramMb = available.ramMb + requests.ramMb,
      diskGb = available.diskGb + requests.diskGb))

    pending.asScala.foreach { process =>
      if (reserveResources(process.name, process.payload.resources)) {
        pending.remove(process)
        val future = schedule(process)
        running(process.name) = Running(process, future)
      }
    }
  }

  private def execute(process: Process): Int = {
    val javaProcess = startProcess(process)
    eventBus.post(ProcessStartedEvent(process.jobName, process.taskName))
    val exitCode = javaProcess.waitFor()
    logger.info(s"Completed execution of process ${process.name} (exit code: $exitCode)")
    exitCode
  }

  private def startProcess(process: Process): JavaProcess = {
    val workDir = getWorkDir(process.name)
    Files.createDirectories(workDir)

    val outputsDir = getOutputsDir(workDir)
    Files.createDirectories(outputsDir)
    val command = createCommandLine(process, getResultFile(workDir))
    logger.debug(s"Command-line for process ${process.name}: ${command.mkString(" ")}")

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

  private def readLogFile(file: Path, skip: Option[Int], tail: Option[Int]): Future[Seq[String]] = {
    Reader
      .readAll(Reader.fromFile(file.toFile))
      .map { case Buf.Utf8(content) =>
        var lines = content.split("\n")
        skip.foreach(n => lines = lines.drop(n))
        tail.foreach(n => lines = lines.takeRight(n))
        lines
      }
  }

  private def createCommandLine(process: Process, outputFile: Path): Seq[String] = {
    val cmd = mutable.ListBuffer.empty[String]
    cmd += javaBinary
    cmd ++= Seq("-cp", executorUri)
    cmd += s"-Xmx${process.payload.resources.ramMb}M"
    cmd += "fr.cnrs.liris.accio.executor.AccioExecutorMain"
    cmd += BinaryScroogeSerializer.toString(process.payload)
    cmd += outputFile.toAbsolutePath.toString
    cmd ++= executorArgs
    cmd.toList
  }

  private def read(file: Path): Option[OpResult] = {
    if (!file.toFile.canRead) {
      logger.warn(s"Result file is not readable: $file")
      None
    } else {
      val fis = new FileInputStream(file.toFile)
      try {
        Some(BinaryScroogeSerializer.read(fis, OpResult))
      } catch {
        case e: Throwable =>
          logger.warn(s"Error while reading result file: $file", e)
          None
      } finally {
        fis.close()
      }
    }
  }
}
