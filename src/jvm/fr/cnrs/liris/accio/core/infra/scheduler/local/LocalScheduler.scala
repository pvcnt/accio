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
import java.util.UUID
import java.util.concurrent.{ConcurrentHashMap, Executors}

import com.twitter.util.ExecutorServiceFuturePool
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.service.{Downloader, Scheduler}
import fr.cnrs.liris.common.util.{FileUtils, HashUtils}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Scheduler executing tasks locally, in the same machine. Each task is started inside a new Java process.
 *
 * Intended for testing or use in single-node development clusters, as it does not support cluster-mode
 * (by definition...) nor resource constraints.
 *
 * @param opRegistry   Operator registry.
 * @param downloader   Downloader.
 * @param workDir      Working directory where sandboxes will be stored.
 * @param executorUri  URI where to fetch the executor.
 * @param javaHome     Java home to be used when running nodes.
 * @param executorArgs Arguments to pass to the executors.
 */
class LocalScheduler(
  opRegistry: OpRegistry,
  downloader: Downloader,
  workDir: Path,
  executorUri: String,
  javaHome: Option[String],
  executorArgs: Seq[String])
  extends Scheduler with StrictLogging {

  private[this] val monitors = new ConcurrentHashMap[String, TaskMonitor].asScala
  private[this] val pool = new ExecutorServiceFuturePool(Executors.newWorkStealingPool)
  private[this] lazy val localExecutorPath = {
    val targetPath = workDir.resolve("executor.jar")
    if (targetPath.toFile.exists()) {
      targetPath.toFile.delete()
    }
    logger.info(s"Downloading executor JAR to ${targetPath.toAbsolutePath}")
    downloader.download(executorUri, targetPath)
    targetPath
  }

  override def submit(runId: RunId, nodeName: String, payload: OpPayload): Task = {
    val id = TaskId(UUID.randomUUID().toString)
    val key = HashUtils.sha1(id.value)
    val monitor = new TaskMonitor(id, key, payload)
    monitors(key) = monitor
    val f = pool(monitor.run())
    f.onSuccess(_ => logger.debug(s"[T${id.value}] Monitoring thread completed"))
    f.onFailure(e => logger.error(s"[T${id.value}] Error in monitoring thread", e))
    logger.debug(s"[T${id.value}] Submitted task")
    Task(
      id = id,
      runId = runId,
      key = key,
      payload = payload,
      nodeName = nodeName,
      createdAt = System.currentTimeMillis(),
      scheduler = "local",
      state = TaskState(TaskStatus.Scheduled))
  }

  override def kill(key: String): Unit = {
    monitors.get(key).foreach(_.kill())
  }

  override def stop(): Unit = {
    monitors.values.foreach(_.kill())
    monitors.clear()
  }

  override def finalize(): Unit = {
    super.finalize()
    stop()
  }

  private def getSandboxPath(key: String) = workDir.resolve(key)

  private class TaskMonitor(taskId: TaskId, key: String, payload: OpPayload) extends Runnable with StrictLogging {
    private[this] var killed = false
    private[this] var process: Option[Process] = None

    override def run(): Unit = {
      val maybeProcess = synchronized {
        process = if (killed) None else Some(startProcess(taskId, key, payload))
        process
      }
      maybeProcess match {
        case None =>
          logger.debug(s"[T${taskId.value}] Skipped task (killed)")
          cleanup()
        case Some(p) =>
          logger.debug(s"[T${taskId.value}] Waiting for process completion")
          try {
            p.waitFor()
            logger.debug(s"[T${taskId.value}] Process completed")
          } catch {
            case e: InterruptedException => logger.warn(s"[T${taskId.value}] Interrupted while waiting", e)
          } finally {
            cleanup()
          }
      }
    }

    def kill(): Unit = synchronized {
      if (!killed) {
        killed = true
        process.foreach(_.destroyForcibly())
        process = None
        logger.debug(s"[T${taskId.value}] Killed task")
      }
    }

    private def cleanup() = {
      monitors.remove(key)
      FileUtils.safeDelete(getSandboxPath(key))
    }

    private def startProcess(taskId: TaskId, key: String, payload: OpPayload): Process = {
      val javaBinary = javaHome.orElse(sys.env.get("JAVA_HOME")).map(home => s"$home/bin/java").getOrElse("/usr/bin/java")
      val resource = opRegistry(payload.op).resource

      val cmd = mutable.ListBuffer.empty[String]
      cmd += javaBinary
      cmd += "-cp"
      cmd += localExecutorPath.toString
      cmd += s"-Xmx${resource.ramMb}M"
      cmd += "fr.cnrs.liris.accio.executor.AccioExecutorMain"
      cmd ++= executorArgs
      cmd += taskId.value

      logger.debug(s"[T${taskId.value}] Command-line: ${cmd.mkString(" ")}")

      val sandboxDir = getSandboxPath(key)
      Files.createDirectories(sandboxDir)

      val pb = new ProcessBuilder().command(cmd: _*).directory(sandboxDir.toFile).inheritIO()

      // Pass as environment variables resource constraints, in case it can help operators to better use them.
      // As an example, SparkleEnv uses the "CPU" variable to known how many cores it can use.
      val env = pb.environment()
      env.put("ACCIO_CPU", resource.cpu.toString)
      env.put("ACCIO_RAM", resource.ramMb.toString)
      env.put("ACCIO_DISK", resource.diskMb.toString)

      pb.start()
    }
  }

}