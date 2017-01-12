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

package fr.cnrs.liris.accio.executor

import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

import com.google.inject.{Inject, Singleton}
import com.twitter.util.{Await, ExecutorServiceFuturePool, Future}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.application.handler.{CompletedTaskRequest, HeartbeatTaskRequest, UpdateTaskRequest}
import fr.cnrs.liris.accio.core.application.{OpExecutor, OpExecutorOpts}
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.thrift.agent._

import scala.collection.mutable

@Singleton
class TaskExecutor @Inject()(opExecutor: OpExecutor, trackerClient: TaskTrackerService.FinagledClient)
  extends StrictLogging {

  private[this] val pool = new ExecutorServiceFuturePool(Executors.newSingleThreadExecutor)
  private[this] val stdoutBytes = new ByteArrayOutputStream
  private[this] val stderrBytes = new ByteArrayOutputStream
  private[this] val stopped = new AtomicBoolean(false)
  private[this] val threads = mutable.Set.empty[Thread]

  def submit(taskId: TaskId, payload: OpPayload): Future[Unit] = {
    threads ++= Set(new HeartbeatThread(taskId), new StreamLogsThread(taskId))
    threads.foreach(_.start())
    logger.info(s"Starting execution of task ${taskId.value}")
    pool {
      val opts = OpExecutorOpts(useProfiler = true)
      opExecutor.execute(payload, opts)
    }.handle {
      case e: Throwable =>
        logger.error("Execution of operator raised an unexpected error", e)
        OpResult(-999, Some(ErrorFactory.create(e)))
    }.flatMap { result =>
      trackerClient.completed(CompletedTaskRequest(taskId, result))
        .onFailure(e => logger.error("Error while marking task as completed", e))
    }.respond { _ =>
      stopped.set(true)
      threads.foreach(_.interrupt())
      logger.info(s"Completed execution of task ${taskId.value}")
    }.unit
  }

  private def extractLogs(baos: ByteArrayOutputStream, taskId: TaskId, classifier: String) = {
    val content = new String(baos.toByteArray)
    baos.reset()
    val at = System.currentTimeMillis
    content.trim
      .split("\n")
      .toSeq
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(line => TaskLog(taskId, classifier, at, line))
  }

  private class StreamLogsThread(taskId: TaskId) extends Thread {
    override def run(): Unit = {
      logger.debug("Logs thread started")
      Thread.sleep(2 * 1000)
      while (!stopped.get()) {
        val logs = extractLogs(stdoutBytes, taskId, "stdout") ++ extractLogs(stderrBytes, taskId, "stderr")
        if (logs.nonEmpty) {
          val future = trackerClient.update(UpdateTaskRequest(taskId, logs = Some(logs)))
            .onFailure(e => logger.error("Error while sending logs", e))
            .onSuccess(_ => logger.debug(s"Sent logs for task $taskId (${logs.size} lines)"))
          Await.ready(future)
        }
        try {
          Thread.sleep(if (logs.nonEmpty) 5 * 1000 else 2 * 1000)
        } catch {
          case _: InterruptedException => // Do nothing.
        }
      }
      logger.debug("Logs thread stopped")
    }
  }

  private class HeartbeatThread(taskId: TaskId) extends Thread {
    override def run(): Unit = {
      logger.debug("Heartbeat thread started")
      Thread.sleep(5 * 1000)
      while (!stopped.get()) {
        val future = trackerClient.heartbeat(HeartbeatTaskRequest(taskId))
          .onFailure(e => logger.error("Error while marking sending heartbeat", e))
          .onSuccess(_ => logger.debug(s"Sent heartbeat for task $taskId"))
        Await.ready(future)
        try {
          Thread.sleep(30 * 1000)
        } catch {
          case _: InterruptedException => // Do nothing.
        }
      }
      logger.debug("Heartbeat thread stopped")
    }
  }

}