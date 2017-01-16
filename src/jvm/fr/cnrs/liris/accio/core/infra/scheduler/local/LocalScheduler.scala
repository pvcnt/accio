/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.{ConcurrentHashMap, Executors}

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
 * @param opRegistry Operator registry.
 * @param downloader Downloader.
 * @param config     Scheduler configuration.
 */
class LocalScheduler(opRegistry: OpRegistry, downloader: Downloader, config: LocalSchedulerConfig)
  extends Scheduler with StrictLogging {

  private[this] val monitors = new ConcurrentHashMap[String, TaskMonitor].asScala
  private[this] val executorService = Executors.newSingleThreadExecutor
  private[this] lazy val localExecutorPath = {
    val targetPath = config.workDir.resolve("executor.jar")
    logger.info(s"Downloading executor JAR to ${targetPath.toAbsolutePath}")
    downloader.download(config.executorUri, targetPath)
    targetPath
  }

  override def submit(runId: RunId, nodeName: String, payload: OpPayload): Task = {
    val id = TaskId(UUID.randomUUID().toString)
    val key = HashUtils.sha1(id.value)
    val monitor = new TaskMonitor(id, key, payload)
    monitors(key) = monitor
    executorService.submit(monitor)
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
    executorService.shutdownNow()
  }

  private def getSandboxPath(key: String) = config.workDir.resolve(key)

  private class TaskMonitor(id: TaskId, key: String, payload: OpPayload) extends Runnable with StrictLogging {
    private[this] var killed = false
    private[this] var process: Option[Process] = None

    override def run(): Unit = {
      val maybeProcess = synchronized {
        process = if (killed) None else Some(startProcess(id, key, payload))
        process
      }
      maybeProcess match {
        case None =>
          logger.info(s"Skipped task ${id.value} (killed)")
          cleanup()
        case Some(p) =>
          logger.info(s"Started task ${id.value}")
          try {
            p.waitFor()
            logger.info(s"Completed task ${id.value}")
          } catch {
            case e: InterruptedException => logger.warn(s"Interrupted while waiting for task ${id.value}", e)
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
        logger.info(s"Killed task ${id.value}")
      }
    }

    private def cleanup() = {
      monitors.remove(key)
      FileUtils.safeDelete(getSandboxPath(key))
    }

    private def startProcess(id: TaskId, key: String, payload: OpPayload): Process = {
      val javaBinary = config.javaHome.orElse(sys.env.get("JAVA_HOME")).map(home => s"$home/bin/java").getOrElse("/usr/bin/java")
      val resource = opRegistry(payload.op).resource

      val cmd = mutable.ListBuffer.empty[String]
      cmd += javaBinary
      cmd += "-cp"
      cmd += localExecutorPath.toString
      cmd += s"-Xmx${resource.ramMb}M"
      cmd += "fr.cnrs.liris.accio.executor.AccioExecutorMain"
      cmd += "-id"
      cmd += id.value
      cmd += "-addr"
      cmd += config.agentAddr
      cmd += "-uploader.type"
      cmd += config.uploaderType
      cmd ++= config.uploaderArgs.flatMap { case (k, v) => Seq(s"uploader.${config.uploaderType}.$k", v) }

      val sandboxDir = getSandboxPath(key)
      Files.createDirectories(sandboxDir)

      val pb = new ProcessBuilder().command(cmd: _*).directory(sandboxDir.toFile).inheritIO()
      pb.start()
    }
  }

}