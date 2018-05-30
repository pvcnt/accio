/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

package fr.cnrs.liris.accio.executor

import java.io.FileInputStream
import java.nio.file.{Files, Path}
import java.util.concurrent.{ConcurrentHashMap, Executors}

import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.domain.thrift.ThriftAdapter
import fr.cnrs.liris.accio.domain.{OpPayload, OpResult, thrift}
import fr.cnrs.liris.lumos.domain.{ErrorDatum, RemoteFile}
import fr.cnrs.liris.util.jvm.JavaHome
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.control.NonFatal

final class TaskExecutor(workDir: Path, handler: TaskLifecycleHandler = NullTaskLifecycleHandler)
  extends Logging {

  private[this] val pool = Executors.newSingleThreadExecutor(new NamedPoolThreadFactory("scheduler"))
  private[this] val tasks = new ConcurrentHashMap[String, TaskThread].asScala

  def submit(name: String, executable: RemoteFile, payload: OpPayload): Unit = {
    val p = new TaskThread(name, executable, payload)
    if (tasks.putIfAbsent(name, p).isEmpty) {
      logger.warn(s"Scheduled task $name")
      pool.submit(p)
    } else {
      logger.warn(s"Task $name was already running")
    }
  }

  def kill(name: String): Unit = {
    tasks.remove(name).foreach(_.kill())
  }

  def close(): Unit = pool.shutdownNow()

  private class TaskThread(val name: String, executable: RemoteFile, val payload: OpPayload)
    extends Runnable {

    private[this] val sandboxDir = workDir.resolve(name)
    private[this] var process: Option[Process] = None
    private[this] var killed = false

    override def run(): Unit = {
      logger.info(s"Starting task $name")
      val started = synchronized {
        if (!killed) {
          process = Some(start())
          true
        } else {
          false
        }
      }
      try {
        if (started) {
          logger.info(s"Started task $name")
          handler.taskStarted(name)
          val exitCode = process.get.waitFor()
          if (!killed) {
            logger.info(s"Completed task $name (exit code: $exitCode)")
            handler.taskCompleted(name, exitCode, readResult())
          }
        }
      } catch {
        case NonFatal(e) => logger.error(s"Error while waiting for task $name", e)
      } finally {
        tasks.remove(name)
      }
    }

    def kill(): Unit = {
      synchronized {
        if (!killed) {
          killed = true
          logger.info(s"Killed task $name")
        }
      }
      try {
        process.foreach(_.destroyForcibly())
      } catch {
        case NonFatal(e) => logger.error(s"Error while killing task $name", e)
      }
    }

    private def start(): Process = {
      Files.createDirectories(sandboxDir)
      Files.createDirectories(outputsDir)

      val cmd = mutable.ListBuffer.empty[String]
      if (executable.uri.endsWith(".jar")) {
        cmd += JavaHome.javaBinary.toString
        cmd ++= Seq("-jar", executable.uri)
        if (payload.resources.contains("ramMb")) {
          cmd += s"-Xmm${payload.resources("ramMb")}M"
          cmd += s"-Xmx${payload.resources("ramMb")}M"
        }
        if (payload.resources.contains("cpus")) {
          cmd += s"-com.twitter.jvm.numProcs=${payload.resources("cpus")}"
        }
      } else {
        cmd += executable.uri
      }
      cmd += BinaryScroogeSerializer.toString(ThriftAdapter.toThrift(payload))
      cmd += resultFile.toAbsolutePath.toString
      logger.debug(s"Command-line for task $name: ${cmd.mkString(" ")}")

      new ProcessBuilder()
        .command(cmd: _*)
        .directory(outputsDir.toFile)
        .redirectOutput(sandboxDir.resolve("stdout").toFile)
        .redirectError(sandboxDir.resolve("stderr").toFile)
        .start()
    }

    private def outputsDir = sandboxDir.resolve("outputs")

    private def resultFile = sandboxDir.resolve("result.thrift")

    private def readResult(): OpResult = {
      try {
        val file = resultFile.toFile
        val fis = new FileInputStream(file)
        try {
          ThriftAdapter.toDomain(BinaryScroogeSerializer.read(fis, thrift.OpResult))
        } finally {
          fis.close()
        }
      } catch {
        case NonFatal(e) => OpResult(successful = false, error = Some(ErrorDatum.create(e)))
      }
    }
  }

}
