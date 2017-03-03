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

import java.nio.file.Files
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue, Executors}

import com.google.common.io.ByteStreams
import com.google.inject.{Inject, Singleton}
import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.finagle.stats.{Gauge, StatsReceiver}
import com.twitter.util.{ExecutorServiceFuturePool, Return, StorageUnit, Throw}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain.{Task, TaskId}
import fr.cnrs.liris.accio.core.filesystem.FileSystem
import fr.cnrs.liris.common.util.{FileUtils, Platform}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Scheduler executing tasks locally, in the same machine. Each task is started inside a new Java process. Intended
 * for testing or use in single-node development clusters.
 *
 * @param filesystem    Distributed filesystem.
 * @param statsReceiver Stats receiver.
 * @param config        Scheduler configuration.
 */
@Singleton
final class LocalScheduler @Inject()(
  filesystem: FileSystem,
  statsReceiver: StatsReceiver,
  config: LocalSchedulerConfig) extends Scheduler with StrictLogging {

  // Monitors for currently running tasks.
  private[this] val monitors = new ConcurrentHashMap[String, TaskMonitor].asScala
  // Queue of waiting tasks.
  private[this] val queue = new ConcurrentLinkedQueue[Task]
  // Total amount of system resources.
  private[this] val totalCpu = sys.runtime.availableProcessors
  private[this] val totalRam = Platform.totalMemory
  private[this] val totalDisk = Platform.totalDiskSpace
  // Pool for monitor threads.
  private[this] val pool = new ExecutorServiceFuturePool(Executors.newCachedThreadPool(new NamedPoolThreadFactory("accio/scheduler")))
  // Download the executor locally. It will always be downloaded at startup time, but reused for multiple tasks.
  private[this] lazy val localExecutorPath = {
    val targetPath = config.workDir.resolve("executor.jar")
    if (targetPath.toFile.exists()) {
      targetPath.toFile.delete()
    }
    logger.info(s"Downloading executor JAR to ${targetPath.toAbsolutePath}")
    filesystem.read(config.executorUri, targetPath)
    targetPath.toAbsolutePath
  }
  // Set used for keeping track of gauges (otherwise only weakly referenced).
  private[this] val gauges = createGauges()
  private[this] val completedCounter = statsReceiver.counter("task", "completed")
  private[this] val errorCounter = statsReceiver.counter("task", "error")
  private[this] val stuckCounter = statsReceiver.counter("task", "stuck")
  logger.debug(s"Detected resources: CPU $totalCpu, RAM ${totalRam.map(_.inMegabytes + "Mb").getOrElse("-")}, disk ${totalDisk.map(_.inMegabytes + "Mb").getOrElse("-")}")

  override def submit(task: Task): String = {
    synchronized {
      if (isSchedulable(task)) {
        start(task)
      } else {
        queue.add(task)
        if (monitors.isEmpty) {
          // This job will never be scheduled...
          stuckCounter.incr()
          logger.error(s"Not enough resource to schedule job")
        }
      }
    }
    task.id.value
  }

  override def kill(key: String): Unit = synchronized {
    monitors.get(key) match {
      case Some(monitor) =>
        monitor.kill()
        logger.debug(s"[T$key] Killed running task")
      case None =>
        val it = queue.iterator
        while (it.hasNext) {
          if (it.next.id.value == key) {
            it.remove()
          }
        }
        logger.debug(s"[T$key] Killed waiting task")
    }

    // Try to start subsequent tasks.
    startNext()
  }

  override def close(): Unit = synchronized {
    queue.clear()
    monitors.values.foreach(_.kill())
    monitors.clear()
  }

  /**
   * Start all tasks that can be started, w.r.t. to resource constraints.
   */
  private def startNext(): Unit = {
    synchronized {
      val it = queue.iterator
      while (it.hasNext) {
        val job = it.next()
        if (isSchedulable(job)) {
          start(job)
          it.remove()
        }
      }
    }
    if (monitors.isEmpty && !queue.isEmpty) {
      // Some jobs will never be scheduled...
      stuckCounter.incr(queue.size)
      logger.error(s"Not enough resources to schedule any job")
    }
  }

  /**
   * Return total number of CPUs reserved by running tasks.
   */
  private def reservedCpu = monitors.values.map(_.task.resource.cpu).sum

  /**
   * Return total RAM reserved by running tasks.
   */
  private def reservedRam = StorageUnit.fromMegabytes(monitors.values.map(_.task.resource.ramMb).sum)

  /**
   * Return total disk space reserved by running tasks.
   */
  private def reservedDisk = StorageUnit.fromMegabytes(monitors.values.map(_.task.resource.diskMb).sum)

  /**
   * Start a task as an external process. This method should be called from a synchronized section.
   *
   * @param task Task to start.
   */
  private def start(task: Task): Unit = {
    val monitor = new TaskMonitor(task)
    logger.debug(s"[T${task.id.value}] Starting task")
    monitors(monitor.key) = monitor
    pool(monitor.run())
      .respond {
        case Throw(e) =>
          logger.error(s"[T${task.id.value}] Error in monitoring thread", e)
          completedCounter.incr()
          errorCounter.incr()
          startNext()
        case Return(_) =>
          logger.debug(s"[T${task.id.value}] Monitoring thread completed")
          completedCounter.incr()
          startNext()
      }
  }

  /**
   * Check if there is enough resources left to launch a task.
   *
   * @param task Candidate task.
   * @return True if there is enough resources, false otherwise.
   */
  private def isSchedulable(task: Task): Boolean = {
    task.resource.cpu <= (totalCpu - reservedCpu) &&
      totalRam.forall(ram => task.resource.ramMb <= (ram - reservedRam).inMegabytes) &&
      totalDisk.forall(disk => task.resource.diskMb <= (disk - reservedDisk).inMegabytes)
  }

  /**
   * Return the path to the sandbox for a given key.
   *
   * @param id Task identifier.
   */
  private def getSandboxPath(id: TaskId) = config.workDir.resolve(id.value)

  private class TaskMonitor(val task: Task) extends Runnable with StrictLogging {
    private[this] var killed = false
    private[this] var process: Option[Process] = None

    override def run(): Unit = {
      val maybeProcess = synchronized {
        process = if (killed) None else Some(startProcess(task))
        process
      }
      maybeProcess match {
        case None =>
          logger.debug(s"[T${task.id.value}] Skipped task (killed)")
          cleanup()
        case Some(p) =>
          logger.debug(s"[T${task.id.value}] Waiting for process completion")
          try {
            ByteStreams.copy(p.getInputStream, ByteStreams.nullOutputStream)
            p.waitFor()
          } finally {
            cleanup()
          }
      }
    }

    def key: String = task.id.value

    def kill(): Unit = synchronized {
      if (!killed) {
        killed = true
        process.foreach { p =>
          p.destroyForcibly()
          p.waitFor()
        }
        cleanup()
        logger.debug(s"[T${task.id.value}] Killed task")
      }
    }

    private def cleanup() = {
      process = None
      monitors.remove(key)
      FileUtils.safeDelete(getSandboxPath(task.id))
    }

    private def startProcess(task: Task): Process = {
      val cmd = createCommandLine(task)
      logger.debug(s"[T${task.id.value}] Command-line: ${cmd.mkString(" ")}")

      val sandboxDir = getSandboxPath(task.id)
      Files.createDirectories(sandboxDir)

      val pb = new ProcessBuilder()
        .command(cmd: _*)
        .directory(sandboxDir.toFile)
        .redirectErrorStream(true)

      pb.start()
    }
  }

  private def createGauges(): Set[Gauge] = {
    val stats = statsReceiver.scope("accio", "sched")
    val gauges = mutable.Set.empty[Gauge]

    gauges += stats.addGauge("cpu", "max")(totalCpu)
    totalRam.foreach { ram =>
      gauges += stats.addGauge("ram", "max")(ram.inBytes)
    }
    totalDisk.foreach { disk =>
      gauges += stats.addGauge("disk", "max")(disk.inBytes)
    }

    gauges += stats.addGauge("cpu", "available")(totalCpu - reservedCpu.toFloat)
    totalRam.foreach { ram =>
      gauges += stats.addGauge("ram", "available")((ram - reservedRam).inBytes)
    }
    totalDisk.foreach { disk =>
      gauges += stats.addGauge("disk", "available")((disk - reservedDisk).inBytes)
    }

    gauges += stats.addGauge("cpu", "reserved")(reservedCpu.toFloat)
    gauges += stats.addGauge("ram", "reserved")(reservedRam.inBytes)
    gauges += stats.addGauge("disk", "reserved")(reservedDisk.inBytes)

    gauges += stats.addGauge("task", "waiting")(queue.size)
    gauges += stats.addGauge("task", "running")(monitors.size)

    gauges.toSet
  }

  private def createCommandLine(task: Task): Seq[String] = {
    val args = config.executorArgs ++ Seq("-addr", config.agentAddr)
    val javaBinary = config.javaHome.orElse(sys.env.get("JAVA_HOME")).map(home => s"$home/bin/java").getOrElse("/usr/bin/java")
    val cmd = mutable.ListBuffer.empty[String]
    cmd += javaBinary
    cmd ++= Seq("-cp", localExecutorPath.toString)
    cmd += s"-Xmx${task.resource.ramMb}M"
    cmd += "fr.cnrs.liris.accio.executor.AccioExecutorMain"
    cmd ++= args
    cmd ++= Seq("-com.twitter.jvm.numProcs", task.resource.cpu.toString)
    cmd += task.id.value
  }
}