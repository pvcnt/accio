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

package fr.cnrs.liris.accio.core.runtime

import java.io.FileOutputStream
import java.nio.file.{Files, Path}
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import java.util.concurrent.{ConcurrentHashMap, Executors}

import com.twitter.finatra.json.FinatraObjectMapper
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.framework.{OpDef, OpRegistry}
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.collection.mutable

case class LocalSchedulerConfig(accioJarUri: String)

class LocalScheduler(opRegistry: OpRegistry, mapper: FinatraObjectMapper, config: LocalSchedulerConfig, workDir: Path) extends Scheduler {
  private[this] val statuses = new ConcurrentHashMap[JobId, JobStatus].asScala
  private[this] val monitors = new ConcurrentHashMap[JobId, JobMonitor].asScala
  private[this] val executor = Executors.newSingleThreadExecutor()
  private[this] val javaBinary = sys.env.get("JAVA_HOME").map(_ + "/bin/java").getOrElse("java")

  //TODO: download local Accio JAR.

  override def schedule(jobs: Seq[Job]): Unit = {
    jobs.foreach(schedule(_))
  }

  override def kill(id: JobId): Unit = {
    monitors.get(id).foreach(_.kill())
  }

  override def stop(): Unit = {
    monitors.values.foreach(_.kill())
    executor.shutdown()
  }

  override def get(id: JobId): Option[JobStatus] = statuses.get(id)

  private def schedule(job: Job) = {
    opRegistry.get(job.node.op) match {
      case None => throw new RuntimeException(s"Cannot schedule unknown operator: ${job.node.op}")
      case Some(op) =>
        val sandboxDir = workDir.resolve(job.id.value)
        Files.createDirectories(sandboxDir)
        val jobFile = sandboxDir.resolve("_job.json")
        mapper.writeValue(job, new FileOutputStream(jobFile.toFile))
        val cmd = buildCommand(op.defn, jobFile.toAbsolutePath.toString)

        val pb = new ProcessBuilder()
          .command(cmd: _*)
          .directory(sandboxDir.toFile)
          .redirectOutput(sandboxDir.resolve("_stdout").toFile)
          .redirectError(sandboxDir.resolve("_stderr").toFile)

        statuses(job.id) = JobStatus(DateTime.now, None, None, None, None)
        monitors(job.id) = new JobMonitor(job.id, pb)
        executor.submit(monitors(job.id))
    }
  }

  private def buildCommand(opDef: OpDef, jobFileUri: String) = {
    val cmd = mutable.ListBuffer.empty[String]
    cmd += javaBinary
    cmd += s"-Xmx${
      opDef.resource.memory.toHuman
    }"
    cmd += s"-cp ${
      config.accioJarUri
    }"
    cmd += "fr.cnrs.liris.accio.core.runtime.NodeExecutor"
    cmd += jobFileUri
  }

  private class JobMonitor(id: JobId, pb: ProcessBuilder) extends Runnable with StrictLogging {
    private[this] val state = new AtomicInteger(0)
    private[this] val process = new AtomicReference[Process](null)

    def kill(): Unit = {
      if (state.compareAndSet(0, 2)) {
        statuses(id) = statuses(id).copy(exitCode = Some(-999))
        logger.debug(s"Job $id has been killed before starting")
      } else if (state.compareAndSet(1, 2)) {
        Option(process.getAndSet(null)).foreach(_.destroyForcibly())
        statuses(id) = statuses(id).copy(completedAt = Some(DateTime.now), exitCode = Some(-999))
        logger.debug(s"Job $id has been killed while running")
      }
      monitors.remove(id)
    }

    override def run(): Unit = {
      if (state.compareAndSet(0, 1)) {
        process.set(pb.start())
        statuses(id) = statuses(id).copy(startedAt = Some(DateTime.now))
        logger.debug(s"Job $id has started")

        try {
          Option(process.get).foreach(_.waitFor())
          if (state.compareAndSet(1, 2)) {
            statuses(id) = statuses(id).copy(completedAt = Some(DateTime.now), exitCode = Option(process.get).map(_.exitValue))
            logger.debug(s"Job $id has completed")
          }
          //TODO: relaunch in case of an error (but not killed).
        } catch {
          case e: InterruptedException =>
            if (state.compareAndSet(1, 2)) {
              statuses(id) = statuses(id).copy(completedAt = Some(DateTime.now), exitCode = Some(-999))
              logger.warn(s"Job $id has been interrupted", e)
            }
        }
        monitors.remove(id)
      }
    }
  }

}