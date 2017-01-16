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

import java.io.{ByteArrayOutputStream, PrintStream}
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

import com.google.inject.{Inject, Singleton}
import com.twitter.util._
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent.AgentService
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.service.handler.{CompleteTaskRequest, HeartbeatRequest, StartTaskRequest, StreamLogsRequest}
import fr.cnrs.liris.accio.core.service.{OpExecutor, OpExecutorOpts}

import scala.collection.mutable

/**
 * Execute tasks.
 *
 * @param opExecutor
 * @param client
 */
@Singleton
class TaskExecutor @Inject()(opExecutor: OpExecutor, client: AgentService.FinagledClient)
  extends StrictLogging {

  private[this] val pool = new ExecutorServiceFuturePool(Executors.newSingleThreadExecutor)
  private[this] val stdoutBytes = new ByteArrayOutputStream
  private[this] val stderrBytes = new ByteArrayOutputStream
  private[this] val stopped = new AtomicBoolean(false)
  private[this] val threads = mutable.Set.empty[Thread]

  def execute(taskId: TaskId): Future[Unit] = {
    client.startTask(StartTaskRequest(taskId)).transform {
      case Throw(e) =>
        logger.error("Error while registering executor", e)
        Future.Done
      case Return(resp) =>
        logger.debug(s"[T${taskId.value}] Received payload")
        start(taskId, resp.runId, resp.nodeName, resp.payload)
    }
  }

  private def start(taskId: TaskId, runId: RunId, nodeName: String, payload: OpPayload): Future[Unit] = {
    System.setOut(new PrintStream(stdoutBytes))
    System.setErr(new PrintStream(stderrBytes))
    threads ++= Set(new HeartbeatThread(taskId), new StreamLogsThread(taskId, runId, nodeName))
    threads.foreach(_.start())
    logger.debug(s"[T${taskId.value}] Started execution")
    pool {
      val opts = OpExecutorOpts(useProfiler = true, logsPrefix = s"[T${taskId.value}]")
      opExecutor.execute(payload, opts)
    }.transform {
      case Throw(e) =>
        logger.error(s"[T${taskId.value}] Operator raised an unexpected error", e)
        Future(OpResult(-999, Some(ErrorFactory.create(e))))
      case Return(result) =>
        client.completeTask(CompleteTaskRequest(taskId, result))
          .onFailure(e => logger.error(s"[T${taskId.value}] Error while marking task as completed", e))
    }.respond { _ =>
      stopped.set(true)
      threads.foreach(_.interrupt())
      logger.debug(s"[T${taskId.value}] Completed execution")
    }.unit
  }

  private class StreamLogsThread(taskId: TaskId, runId: RunId, nodeName: String) extends Thread {
    override def run(): Unit = {
      logger.debug(s"[T${taskId.value}] Logs thread started")
      trySleep(Duration.fromSeconds(2))
      while (!stopped.get()) {
        val logs = extractLogs(stdoutBytes, "stdout") ++ extractLogs(stderrBytes, "stderr")
        if (logs.nonEmpty) {
          val future = client.streamLogs(StreamLogsRequest(logs))
            .onFailure(e => logger.error(s"[T${taskId.value}] Error while sending logs", e))
            .onSuccess(_ => logger.debug(s"[T${taskId.value}] Sent ${logs.size} logs"))
          Await.ready(future)
          trySleep(Duration.fromSeconds(5))
        } else {
          trySleep(Duration.fromSeconds(2))
        }
      }
      logger.debug(s"[T${taskId.value}] Logs thread stopped")
    }

    private def extractLogs(baos: ByteArrayOutputStream, classifier: String) = {
      val content = new String(baos.toByteArray)
      baos.reset()
      val at = System.currentTimeMillis
      content.trim
        .split("\n")
        .toSeq
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(line => RunLog(runId, nodeName, at, classifier, line))
    }
  }

  private class HeartbeatThread(taskId: TaskId) extends Thread {
    override def run(): Unit = {
      logger.debug(s"[T${taskId.value}] Heartbeat thread started")
      trySleep(Duration.fromSeconds(5))
      while (!stopped.get()) {
        val future = client.heartbeat(HeartbeatRequest(taskId))
          .onFailure(e => logger.error(s"[T${taskId.value}] Error while sending heartbeat", e))
          .onSuccess(_ => logger.debug(s"[T${taskId.value}] Sent heartbeat"))
        Await.ready(future)
        trySleep(Duration.fromSeconds(30))
      }
      logger.debug(s"[T${taskId.value}] Heartbeat thread stopped")
    }
  }

  private def trySleep(duration: Duration) = {
    try {
      Thread.sleep(duration.inMillis)
    } catch {
      case _: InterruptedException => // Do nothing.
    }
  }

}