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

import java.lang.{Process => JavaProcess}
import java.nio.file.{Files, Path}
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{CancellationException, ConcurrentHashMap, ConcurrentLinkedDeque, Executors}

import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.io.{Buf, Reader}
import com.twitter.util.logging.Logging
import com.twitter.util.{Future, FuturePool, Time}
import fr.cnrs.liris.accio.scheduler._
import fr.cnrs.liris.util.Platform

import scala.collection.JavaConverters._

final class LocalScheduler(
  statsReceiver: StatsReceiver,
  reservedResources: Map[String, Long],
  forceScheduling: Boolean,
  dataDir: Path)
  extends Scheduler with Logging {

  Files.createDirectories(dataDir)

  private case class Pending(process: Process)

  private case class Running(process: Process, future: Future[_])

  private[this] val pending = new ConcurrentLinkedDeque[Pending]
  private[this] val running = new ConcurrentHashMap[String, Running].asScala
  private[this] val pool = {
    val executor = Executors.newCachedThreadPool(new NamedPoolThreadFactory("scheduler"))
    FuturePool.interruptible(executor)
  }
  private[this] val totalResources = {
    val available = Map(
      "cpus" -> sys.runtime.availableProcessors.toLong,
      "ramMb" -> Platform.totalMemory.map(_.inMegabytes).getOrElse(0L),
      "diskGb" -> Platform.totalDiskSpace.map(_.inMegabytes).getOrElse(0L))
    available.map { case (k, v) =>
      if (reservedResources.contains(k)) {
        k -> math.max(0, v - reservedResources(k))
      } else {
        k -> v
      }
    }
  }
  // Although the reserve/release code is synchronized, we register available resources as an
  // AtomicReference because it is accessed by the gauges code, which can occur at any time.
  private[this] val availableResources = new AtomicReference(totalResources)

  // Register gauges.
  // Total resources are registered as gauges, even though values are not expected to vary.
  totalResources.keySet.foreach { k =>
    statsReceiver.provideGauge("scheduler", "total", k)(totalResources(k).toFloat)
    statsReceiver.provideGauge("scheduler", "free", k)(availableResources.get().apply(k).toFloat)
  }

  statsReceiver.provideGauge("scheduler", "pending")(pending.size.toFloat)
  statsReceiver.provideGauge("scheduler", "running")(running.size.toFloat)

  override def submit(process: Process): Future[ProcessMetadata] = {
    if (isActive0(process.name) || isCompleted0(process.name)) {
      Future.exception(new IllegalArgumentException(s"Process ${process.name} already exists"))
    } else if (reserveResources(process.name, process.resources)) {
      running(process.name) = Running(process, schedule(process))
      Future.value(ProcessMetadata())
    } else {
      if (!forceScheduling && !isEnoughResources(process.name, process.resources, totalResources)) {
        Future.exception(new RuntimeException(s"Not enough resources to schedule process ${process.name}"))
      } else {
        logger.info(s"Queued process ${process.name}")
        pending.add(Pending(process))
        Future.value(ProcessMetadata())
      }
    }
  }

  override def kill(name: String): Future[Unit] = {
    running.remove(name).foreach { item =>
      item.future.raise(new InterruptedException)
      releaseResources(item.process.resources)
    }
    Future.Done
  }

  override def listLogs(name: String, kind: String, skip: Option[Int], tail: Option[Int]): Future[Seq[String]] = {
    readLogFile(getWorkDir(name).resolve(kind), skip, tail)
  }

  override def isActive(name: String): Future[Boolean] = Future.value(isActive0(name))

  override def isCompleted(name: String): Future[Boolean] = Future.value(isCompleted0(name))

  private def isActive0(name: String): Boolean = {
    pending.iterator.asScala.exists(_.process.name == name) || running.contains(name)
  }

  private def isCompleted0(name: String): Boolean = {
    Files.isDirectory(getWorkDir(name)) && !running.contains(name)
  }

  override def close(deadline: Time): Future[Unit] = Future.Done

  private def schedule(process: Process): Future[Unit] = {
    // We create the working directory outside of the `execute` function because we use the
    // existence or absence of a directory to indicate whether a process with a given name is known
    // to the scheduler. There might be some delay before the task actually starts, the process
    // might be killed in between, but we still want to keep track that a process with this name
    // did exist at some point.
    Files.createDirectories(getWorkDir(process.name))
    pool(execute(process))
      .handle { case _: CancellationException =>
        logger.info(s"Killed execution of process ${process.name}")
      }
      .onFailure { e =>
        logger.error(s"Unexpected error while executing process ${process.name}", e)
      }
      .ensure {
        running.remove(process.name)
        releaseResources(process.resources)
      }
      .unit
  }

  private def isEnoughResources(id: String, requests: Map[String, Long], resources: Map[String, Long]): Boolean = {
    if (requests.forall { case (k, v) => resources.getOrElse(k, 0L) >= v }) {
      true
    } else if (running.isEmpty && forceScheduling) {
      logger.warn(s"Forcing scheduling of task $id")
      true
    } else {
      false
    }
  }

  private def reserveResources(id: String, requests: Map[String, Long]): Boolean = synchronized {
    val available = availableResources.get
    if (!isEnoughResources(id, requests, available)) {
      false
    } else {
      availableResources.set(availableResources.get.map { case (k, v) =>
        if (requests.contains(k)) {
          k -> math.max(0L, v - requests(k))
        } else {
          k -> v
        }
      })
      true
    }
  }

  private def releaseResources(requests: Map[String, Long]): Unit = synchronized {
    availableResources.set(availableResources.get.map { case (k, v) =>
      if (requests.contains(k)) {
        k -> math.min(totalResources(k), v + requests(k))
      } else {
        k -> v
      }
    })
    pending.asScala.zipWithIndex.foreach { case (item, idx) =>
      if (reserveResources(item.process.name, item.process.resources)) {
        val future = schedule(item.process)
        running(item.process.name) = Running(item.process, future)
        pending.remove(idx)
      }
    }
  }

  private def execute(process: Process): Int = {
    val javaProcess = startProcess(process)
    logger.info(s"Started execution of process ${process.name}")
    val exitCode = javaProcess.waitFor()
    logger.info(s"Completed execution of process ${process.name} (exit code: $exitCode)")
    exitCode
  }

  private def startProcess(process: Process): JavaProcess = {
    val workDir = getWorkDir(process.name)
    new ProcessBuilder()
      .command("bash", "-c", process.command)
      .directory(workDir.toFile)
      .redirectOutput(workDir.resolve("stdout").toFile)
      .redirectError(workDir.resolve("stderr").toFile)
      .start()
  }

  private def getWorkDir(name: String): Path = {
    dataDir
      .resolve(name(0).toString)
      .resolve(name(1).toString)
      .resolve(name)
  }

  private def readLogFile(file: Path, skip: Option[Int], tail: Option[Int]): Future[Seq[String]] = {
    Reader
      .readAll(Reader.fromFile(file.toFile))
      .map { case Buf.Utf8(content) =>
        var lines = content.split("\n")
        skip.foreach(n => lines = lines.drop(n))
        tail.foreach(n => lines = lines.takeRight(n))
        // Processes may generate an empty file log, which we do not want to return.
        if (lines.length == 1 && lines.head == "") Seq.empty else lines.toSeq
      }
  }
}
