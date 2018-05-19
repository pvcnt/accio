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
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedDeque, Executors}

import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.io.{Buf, Reader}
import com.twitter.util.logging.Logging
import com.twitter.util.{Future, FuturePool, Time}
import fr.cnrs.liris.accio.domain.Workflow
import fr.cnrs.liris.accio.domain.thrift.ThriftAdapter
import fr.cnrs.liris.accio.scheduler.Scheduler
import fr.cnrs.liris.util.Platform
import fr.cnrs.liris.util.jvm.JavaHome
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

import scala.collection.JavaConverters._
import scala.collection.mutable

final class LocalScheduler(
  statsReceiver: StatsReceiver,
  reservedResources: Map[String, Long],
  executorUri: String,
  forceScheduling: Boolean,
  dataDir: Path)
  extends Scheduler with Logging {

  private case class Pending(workflow: Workflow, args: Seq[String])

  private case class Running(workflow: Workflow, future: Future[_])

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

  override def submit(workflow: Workflow, args: Seq[String]): Unit = {
    if (reserveResources(workflow.name, workflow.resources)) {
      val future = schedule(workflow, args)
      running(workflow.name) = Running(workflow, future)
    } else {
      if (!forceScheduling && !isEnoughResources(workflow.name, workflow.resources, totalResources)) {
        // TODO: We should cancel the task.
        logger.warn(s"Not enough resources to schedule workflow ${workflow.name} (${workflow.resources})")
      }
      logger.info(s"Queued workflow ${workflow.name}")
      pending.add(Pending(workflow, args))
    }
  }

  override def kill(name: String): Unit = {
    running.remove(name).foreach { item =>
      item.future.raise(new RuntimeException)
      releaseResources(item.workflow.resources)
    }
  }

  override def getLogs(name: String, kind: String, skip: Option[Int], tail: Option[Int]): Future[Seq[String]] = {
    readLogFile(getWorkDir(name).resolve(kind), skip, tail)
  }

  override def close(deadline: Time): Future[Unit] = Future.Done

  private def schedule(workflow: Workflow, args: Seq[String]): Future[Unit] = {
    pool(execute(workflow, args))
      .onFailure { e =>
        logger.error(s"Unexpected error while executing workflow ${workflow.name}", e)
      }
      .ensure {
        running.remove(workflow.name)
        releaseResources(workflow.resources)
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
      if (reserveResources(item.workflow.name, item.workflow.resources)) {
        pending.remove(idx)
        val future = schedule(item.workflow, item.args)
        running(item.workflow.name) = Running(item.workflow, future)
      }
    }
  }

  private def execute(workflow: Workflow, args: Seq[String]): Int = {
    val javaProcess = startProcess(workflow, args)
    logger.info(s"Started execution of workflow ${workflow.name}")
    val exitCode = javaProcess.waitFor()
    logger.info(s"Completed execution of workflow ${workflow.name} (exit code: $exitCode)")
    exitCode
  }

  private def startProcess(workflow: Workflow, args: Seq[String]): JavaProcess = {
    val workDir = getWorkDir(workflow.name)
    Files.createDirectories(workDir)

    val cmd = mutable.ListBuffer.empty[String]
    cmd += JavaHome.javaBinary.toString
    cmd += s"-Xmm200M"
    cmd += s"-Xmx200M"
    cmd ++= Seq("-jar", executorUri)
    cmd ++= args
    cmd += BinaryScroogeSerializer.toString(ThriftAdapter.toThrift(workflow))

    new ProcessBuilder()
      .command(cmd: _*)
      .directory(workDir.toFile)
      .redirectOutput(workDir.resolve("stdout").toFile)
      .redirectError(workDir.resolve("stderr").toFile)
      .start()
  }

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
}
