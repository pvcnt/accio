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
import java.util.concurrent.Executors

import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.domain.thrift.ThriftAdapter
import fr.cnrs.liris.accio.domain.{OpPayload, OpResult, thrift}
import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

import scala.collection.mutable
import scala.util.control.NonFatal

final class TaskScheduler(workDir: Path, handler: TaskLifecycleHandler, totalResources: Map[String, Long])
  extends Logging {

  private[this] lazy val javaBinary = {
    sys.env.get("JAVA_HOME")
      .map(home => s"$home/bin/java")
      .getOrElse("/usr/bin/java")
  }
  private[this] val pool = Executors.newCachedThreadPool(new NamedPoolThreadFactory("scheduler"))
  private[this] val pending = mutable.ListBuffer.empty[String]
  private[this] val processes = mutable.Map.empty[String, TaskProcess]
  private[this] val availableResources = mutable.Map.empty[String, Long] ++ totalResources

  def schedule(name: String, executable: RemoteFile, payload: OpPayload): Unit = synchronized {
    val p = new TaskProcess(name, executable, payload)
    processes(name) = p
    if (reserveResources(payload.resources)) {
      pool.submit(p)
    } else {
      pending += name
    }
  }

  def kill(name: String): Unit = synchronized {
    val idx = pending.indexOf(name)
    if (idx > -1) {
      pending.remove(idx)
    }
    processes.remove(name).foreach(_.kill())
  }

  private def reserveResources(resources: Map[String, Long]): Boolean = {
    if (resources.forall { case (k, v) => availableResources.getOrElse(k, 0L) >= v }) {
      resources.foreach { case (k, v) =>
        if (availableResources.contains(k)) {
          availableResources(k) -= v
        }
      }
      true
    } else {
      false
    }
  }

  private def releaseResources(resources: Map[String, Long]): Unit = {
    resources.foreach { case (k, v) =>
      if (availableResources.contains(k)) {
        availableResources(k) += v
      }
    }
  }

  private class TaskProcess(val name: String, executable: RemoteFile, val payload: OpPayload) extends Runnable {
    private[this] var process: Option[Process] = None
    private[this] var killed = false
    private[this] val initLock = new Object
    private[this] val sandboxDir = workDir.resolve(name)

    override def run(): Unit = {
      val started = initLock.synchronized {
        if (!killed) {
          process = Some(startProcess())
          true
        } else {
          false
        }
      }
      try {
        if (started) {
          handler.taskStarted(name)
          val exitCode = process.get.waitFor()
          if (!killed) {
            handler.taskCompleted(name, exitCode, readResult())
          }
        }
      } catch {
        case NonFatal(e) => logger.error(s"Error while waiting for task $name", e)
      } finally {
        terminateProcess(this)
      }
    }

    def kill(): Unit = {
      initLock.synchronized {
        if (!killed) {
          killed = true
        }
      }
      try {
        process.foreach(_.destroyForcibly())
      } catch {
        case NonFatal(e) => logger.error(s"Error while killing task $name", e)
      } finally {
        terminateProcess(this)
      }
    }

    private def startProcess(): Process = {
      Files.createDirectories(sandboxDir)
      Files.createDirectories(outputsDir)

      val command = createCommandLine()
      logger.debug(s"Command-line for task $name: ${command.mkString(" ")}")

      new ProcessBuilder()
        .command(command: _*)
        .directory(outputsDir.toFile)
        .redirectOutput(sandboxDir.resolve("stdout").toFile)
        .redirectError(sandboxDir.resolve("stderr").toFile)
        .start()
    }

    private def createCommandLine(): Seq[String] = {
      val cmd = mutable.ListBuffer.empty[String]
      if (executable.uri.endsWith(".jar")) {
        cmd += javaBinary
        cmd ++= Seq("-cp", executable.uri)
        if (payload.resources.contains("ramMb")) {
          cmd += s"-Xmx${payload.resources("ramMb")}M"
          cmd += s"-Xmm${payload.resources("ramMb")}M"
        }
        if (payload.resources.contains("cpus")) {
          cmd += s"-com.twitter.jvm.numProcs=${payload.resources("cpus")}"
        }
        cmd += "fr.cnrs.liris.accio.executor.AccioExecutorMain"
      } else {
        cmd += executable.uri
      }
      cmd += BinaryScroogeSerializer.toString(ThriftAdapter.toThrift(payload))
      cmd += resultFile.toAbsolutePath.toString
      cmd.toList
    }

    private def terminateProcess(p: TaskProcess): Unit = synchronized {
      releaseResources(p.payload.resources)
      processes.remove(p.name)

      pending.zipWithIndex.foreach { case (name, idx) =>
        val p = processes(name)
        if (reserveResources(p.payload.resources)) {
          pool.submit(p)
          pending.remove(idx)
        }
      }
    }

    private def outputsDir = sandboxDir.resolve("outputs")

    private def resultFile = sandboxDir.resolve("result.thrift")

    private def readResult(): OpResult = {
      val file = resultFile.toFile
      if (!file.canRead) {
        logger.warn(s"Result file is not readable: $file")
        OpResult(successful = false)
      } else {
        val fis = new FileInputStream(file)
        try {
          ThriftAdapter.toDomain(BinaryScroogeSerializer.read(fis, thrift.OpResult))
        } catch {
          case e: Throwable =>
            logger.warn(s"Error while reading result file: $file", e)
            OpResult(successful = false)
        } finally {
          fis.close()
        }
      }
    }
  }

}
