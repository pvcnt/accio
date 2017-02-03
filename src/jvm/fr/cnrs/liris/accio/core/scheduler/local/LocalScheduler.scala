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

package fr.cnrs.liris.accio.core.scheduler.local

import java.nio.file.Files
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue, Executors}

import com.google.common.io.ByteStreams
import com.google.inject.{Inject, Singleton}
import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.{ExecutorServiceFuturePool, Return, StorageUnit, Throw}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain.RunLog
import fr.cnrs.liris.accio.core.downloader.Downloader
import fr.cnrs.liris.accio.core.scheduler.{Job, Scheduler}
import fr.cnrs.liris.accio.core.storage.MutableRunRepository
import fr.cnrs.liris.accio.core.util.Configurable
import fr.cnrs.liris.common.util.{FileUtils, Platform}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Scheduler executing tasks locally, in the same machine. Each task is started inside a new Java process. Intended
 * for testing or use in single-node development clusters.
 *
 * @param downloader    Downloader.
 * @param runRepository Run repository.
 * @param statsReceiver Stats receiver.
 */
@Singleton
class LocalScheduler @Inject()(
  downloader: Downloader,
  runRepository: MutableRunRepository,
  statsReceiver: StatsReceiver)
  extends Scheduler
    with Configurable[LocalSchedulerConfig]
    with StrictLogging {

  // Monitors for currently running tasks.
  private[this] val monitors = new ConcurrentHashMap[String, TaskMonitor].asScala
  // Queue of waiting tasks.
  private[this] val queue = new ConcurrentLinkedQueue[Job]
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
    downloader.download(config.executorUri, targetPath)
    targetPath.toAbsolutePath
  }
  // Set used for keeping track of gauges (otherwise only weakly referenced).
  private[this] val gauges = mutable.Set.empty[Any]
  // Set up telemetry.
  registerStats()
  logger.debug(s"Detected resources: CPU $totalCpu, RAM ${totalRam.map(_.inMegabytes + "Mb").getOrElse("-")}, disk ${totalDisk.map(_.inMegabytes + "Mb").getOrElse("-")}")

  override def configClass: Class[LocalSchedulerConfig] = classOf[LocalSchedulerConfig]

  override def submit(job: Job): String = {
    synchronized {
      if (isEnoughResource(job)) {
        startProcess(job)
      } else {
        queue.add(job)
        if (monitors.isEmpty) {
          // It is an error because the system is stuck and cannot recover.
          logger.error(s"Not enough resource to schedule job")
        }
      }
    }
    job.taskId.value
  }

  override def kill(key: String): Unit = synchronized {
    monitors.get(key) match {
      case Some(monitor) =>
        monitor.kill()
        logger.debug(s"[T$key] Killed running task")
      case None =>
        val it = queue.iterator
        while (it.hasNext) {
          if (it.next.taskId.value == key) {
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
        if (isEnoughResource(job)) {
          startProcess(job)
          it.remove()
        }
      }
    }
    if (monitors.isEmpty && !queue.isEmpty) {
      // It is an error because the system is stuck and cannot recover.
      logger.error(s"Not enough resources to schedule any job")
    }
  }

  /**
   * Return total number of CPUs reserved by running tasks.
   */
  private def reservedCpu = monitors.values.map(_.job.resource.cpu).sum

  /**
   * Return total RAM reserved by running tasks.
   */
  private def reservedRam = StorageUnit.fromMegabytes(monitors.values.map(_.job.resource.ramMb).sum)

  /**
   * Return total disk space reserved by running tasks.
   */
  private def reservedDisk = StorageUnit.fromMegabytes(monitors.values.map(_.job.resource.diskMb).sum)

  /**
   * Start a job as an external process. This method should be called from a synchronized section.
   *
   * @param job Job to start.
   */
  private def startProcess(job: Job): Unit = {
    val monitor = new TaskMonitor(job)
    logger.debug(s"[T${job.taskId.value}] Starting task")
    monitors(job.taskId.value) = monitor
    pool(monitor.run())
      .respond {
        case Throw(e) =>
          logger.error(s"[T${job.taskId.value}] Error in monitoring thread", e)
          startNext()
        case Return(_) =>
          logger.debug(s"[T${job.taskId.value}] Monitoring thread completed")
          startNext()
      }
  }

  /**
   * Check if there is enough resources left to launch a job.
   *
   * @param job Candidate job.
   * @return True if there is enough resources, false otherwise.
   */
  private def isEnoughResource(job: Job): Boolean = {
    job.resource.cpu <= (totalCpu - reservedCpu) &&
      totalRam.forall(ram => job.resource.ramMb <= (ram - reservedRam).inMegabytes) &&
      totalDisk.forall(disk => job.resource.diskMb <= (disk - reservedDisk).inMegabytes)
  }

  /**
   * Return the path to the sandbox for a given key.
   *
   * @param key Task key.
   */
  private def getSandboxPath(key: String) = config.workDir.resolve(key)

  private class TaskMonitor(val job: Job) extends Runnable with StrictLogging {
    private[this] var killed = false
    private[this] var process: Option[Process] = None

    override def run(): Unit = {
      val maybeProcess = synchronized {
        process = if (killed) None else Some(startProcess(job))
        process
      }
      maybeProcess match {
        case None =>
          logger.debug(s"[T${job.taskId.value}] Skipped task (killed)")
          cleanup()
        case Some(p) =>
          logger.debug(s"[T${job.taskId.value}] Waiting for process completion")
          try {
            // TODO: have a thread consuming output, separating between stdout and stderr.
            val out = new String(ByteStreams.toByteArray(p.getInputStream))
            p.waitFor()
            val logs = out.trim.split("\n").map { line =>
              RunLog(job.runId, job.nodeName, System.currentTimeMillis(), "stdout", line.trim)
            }
            if (logs.nonEmpty) {
              logger.debug(s"[T${job.taskId.value}] Saving ${logs.length} additional logs")
              runRepository.save(logs)
            }
          } catch {
            case e: InterruptedException => logger.warn(s"[T${job.taskId.value}] Interrupted while waiting", e)
          } finally {
            cleanup()
          }
      }
    }

    def kill(): Unit = synchronized {
      if (!killed) {
        killed = true
        process.foreach(_.destroyForcibly())
        cleanup()
        logger.debug(s"[T${job.taskId.value}] Killed task")
      }
    }

    private def cleanup() = {
      process = None
      monitors.remove(job.taskId.value)
      FileUtils.safeDelete(getSandboxPath(job.taskId.value))
    }

    private def startProcess(job: Job): Process = {
      val cmd = createCommandLine(
        job,
        localExecutorPath.toString,
        config.executorArgs ++ Seq("-addr", config.agentAddr),
        config.javaHome.orElse(sys.env.get("JAVA_HOME")))
      logger.debug(s"[T${job.taskId.value}] Command-line: ${cmd.mkString(" ")}")

      val sandboxDir = getSandboxPath(job.taskId.value)
      Files.createDirectories(sandboxDir)

      val pb = new ProcessBuilder()
        .command(cmd: _*)
        .directory(sandboxDir.toFile)
        .redirectErrorStream(true)

      pb.start()
    }
  }

  private def registerStats(): Unit = {
    val stats = statsReceiver.scope("accio", "sched")

    gauges += stats.addGauge("cpu", "max")(totalCpu)
    gauges += totalRam.foreach(ram => stats.addGauge("ram", "max")(ram.inBytes))
    gauges += totalDisk.foreach(disk => stats.addGauge("disk", "max")(disk.inBytes))

    gauges += stats.addGauge("cpu", "available")(totalCpu - reservedCpu.toFloat)
    gauges += totalRam.foreach(ram => stats.addGauge("ram", "available")((ram - reservedRam).inBytes))
    gauges += totalDisk.foreach(disk => stats.addGauge("disk", "available")((disk - reservedDisk).inBytes))

    gauges += stats.addGauge("cpu", "reserved")(reservedCpu.toFloat)
    gauges += stats.addGauge("ram", "reserved")(reservedRam.inBytes)
    gauges += stats.addGauge("disk", "reserved")(reservedDisk.inBytes)

    gauges += stats.addGauge("waiting")(queue.size)
    gauges += stats.addGauge("running")(monitors.size)
  }

}