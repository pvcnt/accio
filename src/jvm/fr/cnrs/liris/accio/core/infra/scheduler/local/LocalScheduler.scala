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

package fr.cnrs.liris.accio.core.infra.scheduler.local

import java.nio.file.{Files, Path}
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue, Executors}

import com.google.common.io.ByteStreams
import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.util.{ExecutorServiceFuturePool, Return, Throw}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain.{RunLog, RunRepository}
import fr.cnrs.liris.accio.core.service.{Downloader, Job, Scheduler}
import fr.cnrs.liris.common.util.{FileUtils, Platform}

import scala.collection.JavaConverters._

/**
 * Scheduler executing tasks locally, in the same machine. Each task is started inside a new Java process.
 *
 * Intended for testing or use in single-node development clusters, as it does not support cluster-mode
 * (by definition...) nor resource constraints.
 *
 * @param downloader    Downloader.
 * @param workDir       Working directory where sandboxes will be stored.
 * @param executorUri   URI where to fetch the executor.
 * @param javaHome      Java home to be used when running nodes.
 * @param executorArgs  Arguments to pass to the executors.
 * @param runRepository Run repository.
 */
class LocalScheduler(
  downloader: Downloader,
  workDir: Path,
  executorUri: String,
  javaHome: Option[String],
  executorArgs: Seq[String],
  runRepository: RunRepository)
  extends Scheduler with StrictLogging {

  private[this] val monitors = new ConcurrentHashMap[String, TaskMonitor].asScala
  private[this] val queue = new ConcurrentLinkedQueue[Job]
  private[this] val totalCpu = sys.runtime.availableProcessors
  private[this] val totalRam = Platform.totalMemory
  private[this] val totalDisk = Platform.totalDiskSpace
  private[this] val pool = new ExecutorServiceFuturePool(Executors.newCachedThreadPool(new NamedPoolThreadFactory("scheduler")))
  private[this] lazy val localExecutorPath = {
    val targetPath = workDir.resolve("executor.jar")
    if (targetPath.toFile.exists()) {
      targetPath.toFile.delete()
    }
    logger.info(s"Downloading executor JAR to ${targetPath.toAbsolutePath}")
    downloader.download(executorUri, targetPath)
    targetPath.toAbsolutePath
  }

  logger.info(s"Available CPU: $totalCpu, RAM: ${totalRam.map(_.toHuman).getOrElse("<unknown>")}, disk: ${totalDisk.map(_.toHuman).getOrElse("<unknown>")}")

  override def submit(job: Job): String = {
    synchronized {
      if (isSatisfied(job)) {
        start(job)
      } else {
        queue.add(job)
        if (monitors.isEmpty) {
          logger.warn(s"Unable to schedule job: $job, CPU: $totalCpu, RAM: ${totalRam.map(_.toHuman).getOrElse("-")}, disk: ${totalDisk.map(_.toHuman).getOrElse("-")}")
        } else {
          logger.debug(s"Running jobs: ${monitors.size}, waiting jobs: ${queue.size}")
        }
      }
    }
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
  }

  override def stop(): Unit = {
    monitors.values.foreach(_.kill())
    monitors.clear()
  }

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

  private def scheduleNext(): Unit = synchronized {
    val it = queue.iterator
    while (it.hasNext) {
      val job = it.next()
      if (isSatisfied(job)) {
        start(job)
        it.remove()
      }
    }
    if (monitors.isEmpty && !queue.isEmpty) {
      logger.warn(s"Unable to schedule jobs. First job: ${queue.peek}, CPU: $totalCpu, RAM: ${totalRam.map(_.toHuman).getOrElse("-")}, disk: ${totalDisk.map(_.toHuman).getOrElse("-")}")
    } else {
      logger.debug(s"Running jobs: ${monitors.size}, waiting jobs: ${queue.size}")
    }
  }

  private def isSatisfied(job: Job): Boolean = {
    val usedCpu = monitors.values.map(_.job.resource.cpu).sum
    val usedRamMb = monitors.values.map(_.job.resource.ramMb).sum
    val usedDiskMb = monitors.values.map(_.job.resource.diskMb).sum
    job.resource.cpu <= (totalCpu - usedCpu) &&
      totalRam.forall(ram => job.resource.ramMb <= (ram.inMegabytes - usedRamMb)) &&
      totalDisk.forall(disk => job.resource.diskMb <= (disk.inMegabytes - usedDiskMb))
  }

  private def getSandboxPath(key: String) = workDir.resolve(key)

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
            val out = new String(ByteStreams.toByteArray(p.getInputStream))
            p.waitFor()
            val logs = out.trim.split("\n").map { line =>
              RunLog(job.runId, job.nodeName, System.currentTimeMillis(), "stdout", line.trim)
            }
            if (logs.nonEmpty) {
              logger.debug(s"[T${job.taskId.value}] Saved ${logs.length} additional logs")
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
      val cmd = createCommandLine(job, localExecutorPath.toString, executorArgs, javaHome.orElse(sys.env.get("JAVA_HOME")))
      logger.debug(s"[T${job.taskId.value}] Command-line: ${cmd.mkString(" ")}")

      val sandboxDir = getSandboxPath(job.taskId.value)
      Files.createDirectories(sandboxDir)

      val pb = new ProcessBuilder()
        .command(cmd: _*)
        .directory(sandboxDir.toFile)
        .redirectErrorStream(true)

      // Pass as environment variables resource constraints, in case it can help operators to better use them.
      // As an example, SparkleEnv uses the "CPU" variable to known how many cores it can use.
      val env = pb.environment()
      env.put("ACCIO_CPU", job.resource.cpu.toString)
      env.put("ACCIO_RAM", job.resource.ramMb.toString)
      env.put("ACCIO_DISK", job.resource.diskMb.toString)

      pb.start()
    }
  }

}