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
import com.twitter.finagle.stats.{Stat, StatsReceiver}
import com.twitter.util.{ExecutorServiceFuturePool, Return, StorageUnit, Throw}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain.RunLog
import fr.cnrs.liris.accio.core.downloader.Downloader
import fr.cnrs.liris.accio.core.scheduler.{Job, Scheduler}
import fr.cnrs.liris.accio.core.storage.MutableRunRepository
import fr.cnrs.liris.accio.core.util.Configurable
import fr.cnrs.liris.common.util.{FileUtils, Platform}

import scala.collection.JavaConverters._

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

  private[this] val monitors = new ConcurrentHashMap[String, TaskMonitor].asScala
  private[this] val queue = new ConcurrentLinkedQueue[Job]
  private[this] val totalCpu = sys.runtime.availableProcessors
  private[this] val totalRam = Platform.totalMemory
  private[this] val totalDisk = Platform.totalDiskSpace
  private[this] val pool = new ExecutorServiceFuturePool(Executors.newCachedThreadPool(new NamedPoolThreadFactory("accio/scheduler")))
  private[this] val stats = LocalScheduler.Stats(statsReceiver)
  private[this] lazy val localExecutorPath = {
    val targetPath = config.workDir.resolve("executor.jar")
    if (targetPath.toFile.exists()) {
      targetPath.toFile.delete()
    }
    logger.info(s"Downloading executor JAR to ${targetPath.toAbsolutePath}")
    downloader.download(config.executorUri, targetPath)
    targetPath.toAbsolutePath
  }
  recordStats()

  override def configClass: Class[LocalSchedulerConfig] = classOf[LocalSchedulerConfig]

  override def submit(job: Job): String = {
    synchronized {
      if (isEnoughResource(job)) {
        start(job)
      } else {
        queue.add(job)
        if (monitors.isEmpty) {
          logger.error(s"Not enough resource to schedule job")
        }
      }
    }
    recordStats()
    job.taskId.value
  }

  override def kill(key: String): Unit = synchronized {
    monitors.get(key).foreach(_.kill())
    val it = queue.iterator
    while (it.hasNext) {
      if (it.next.taskId.value == key) {
        it.remove()
      }
    }
    recordStats()
  }

  override def close(): Unit = {
    monitors.values.foreach(_.kill())
    monitors.clear()
  }

  private def scheduleNext(): Unit = {
    synchronized {
      val it = queue.iterator
      while (it.hasNext) {
        val job = it.next()
        if (isEnoughResource(job)) {
          start(job)
          it.remove()
        }
      }
    }
    if (monitors.isEmpty && !queue.isEmpty) {
      logger.warn(s"Not enough resources to schedule any job")
    }
    recordStats()
  }

  private def usedCpu = monitors.values.map(_.job.resource.cpu).sum

  private def usedRam = StorageUnit.fromMegabytes(monitors.values.map(_.job.resource.ramMb).sum)

  private def usedDisk = StorageUnit.fromMegabytes(monitors.values.map(_.job.resource.diskMb).sum)

  private def start(job: Job): Unit = {
    // We do not synchronized here, are it is already called from synchronized sections.
    val monitor = new TaskMonitor(job)
    logger.debug(s"[T${job.taskId.value}] Starting task")
    monitors(job.taskId.value) = monitor
    pool(monitor.run())
      .respond {
        case Throw(e) =>
          logger.error(s"[T${job.taskId.value}] Error in monitoring thread", e)
          scheduleNext()
        case Return(_) =>
          logger.debug(s"[T${job.taskId.value}] Monitoring thread completed")
          scheduleNext()
      }
  }

  private def isEnoughResource(job: Job): Boolean = {
    job.resource.cpu <= (totalCpu - usedCpu) &&
      totalRam.forall(ram => job.resource.ramMb <= (ram - usedRam).inMegabytes) &&
      totalDisk.forall(disk => job.resource.diskMb <= (disk - usedDisk).inMegabytes)
  }

  private def getSandboxPath(key: String) = config.workDir.resolve(key)

  private def recordStats() = {
    stats.availableCpu.add(totalCpu)
    totalRam.foreach(ram => stats.availableRam.add(ram.inBytes))
    totalDisk.foreach(disk => stats.availableDisk.add(disk.inBytes))

    stats.usedCpu.add(usedCpu.toFloat)
    stats.usedRam.add(usedRam.inBytes)
    stats.usedDisk.add(usedDisk.inBytes)

    stats.waiting.add(queue.size)
    stats.running.add(monitors.size)
  }

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

}

/**
 * Utils for [[LocalScheduler]].
 */
object LocalScheduler {

  private object Stats {
    def apply(statsReceiver: StatsReceiver): Stats = {
      Stats(
        statsReceiver.scope("sched").stat("running"),
        statsReceiver.scope("sched").stat("waiting"),
        statsReceiver.scope("sched").scope("cpu").stat("used"),
        statsReceiver.scope("sched").scope("cpu").stat("available"),
        statsReceiver.scope("sched").scope("ram").stat("used_mb"),
        statsReceiver.scope("sched").scope("ram").stat("available"),
        statsReceiver.scope("sched").scope("disk").stat("used"),
        statsReceiver.scope("sched").scope("disk").stat("available"))
    }
  }

  /**
   * Runtime metrics.
   *
   * @param running       Number of running jobs.
   * @param waiting       Number of queued jobs.
   * @param usedCpu       CPU used by running jobs.
   * @param availableCpu  CPU available for new jobs.
   * @param usedRam       RAM used by running jobs, in bytes.
   * @param availableRam  RAM available for new jobs, in bytes.
   * @param usedDisk      Disk used by running jobs, in bytes.
   * @param availableDisk Disk availble for new jobs, in bytes.
   */
  private case class Stats(
    running: Stat,
    waiting: Stat,
    usedCpu: Stat,
    availableCpu: Stat,
    usedRam: Stat,
    availableRam: Stat,
    usedDisk: Stat,
    availableDisk: Stat)

}