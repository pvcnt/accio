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

package fr.cnrs.liris.accio.executor

import java.io.{ByteArrayOutputStream, PrintStream}
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

import com.google.inject.Inject
import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.util._
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.runtime.{OpExecutor, OpExecutorOpts}

import scala.collection.mutable

/**
 * Execute tasks. It handles the whole lifecycle of a task:
 *   - getting payload from the agent;
 *   - executing the operator;
 *   - reporting completed task to the agent;
 *   - sending heartbeat to the agent;
 *   - streaming logs to the agent.
 *
 * Although nothing enforces it, it is designed for a single execution of a task.
 *
 * @param opExecutor Operator executor.
 * @param client     Client to the agent.
 */
class TaskExecutor @Inject()(opExecutor: OpExecutor, client: AgentService.FinagledClient) extends StrictLogging {
  private[this] val pool = new ExecutorServiceFuturePool(Executors.newSingleThreadExecutor(new NamedPoolThreadFactory("executor/main")))
  private[this] val threads = mutable.Set.empty[Thread]
  private[this] val stopped = new AtomicBoolean(false)
  private[this] val stdoutBytes = new ByteArrayOutputStream
  private[this] val stderrBytes = new ByteArrayOutputStream

  /**
   * Execute a task.
   *
   * @param taskId Task identifier.
   * @return A future that will be completed once the task is executed.
   */
  def execute(taskId: TaskId): Future[Unit] = {
    client.startTask(StartTaskRequest(taskId)).transform {
      case Throw(InvalidTaskException()) =>
        logger.error(s"[T${taskId.value}] Invalid task")
        Future.Done
      case Throw(e) =>
        logger.error(s"[T${taskId.value}] Error while starting task", e)
        Future.Done
      case Return(resp) => start(taskId, resp.runId, resp.nodeName, resp.payload)
    }
  }

  /**
   * Close the task executor.
   */
  def close(): Unit = {
    stopped.set(true)
    pool.executor.shutdownNow()
    threads.foreach(_.interrupt())
  }

  private def start(taskId: TaskId, runId: RunId, nodeName: String, payload: OpPayload): Future[Unit] = {
    // Swap output streams.
    System.setOut(new PrintStream(stdoutBytes))
    System.setErr(new PrintStream(stderrBytes))

    threads ++= Set(new HeartbeatThread(taskId), new StreamLogsThread(taskId, runId, nodeName))
    threads.foreach(_.start())
    pool {
      val opts = OpExecutorOpts(useProfiler = true)
      opExecutor.execute(payload, opts)
    }.handle {
      case e: Throwable =>
        logger.error(s"[T${taskId.value}] Operator raised an unexpected error", e)
        OpResult(-999, Some(Errors.create(e)))
    }.flatMap { result =>
      client.completeTask(CompleteTaskRequest(taskId, result))
        .onFailure(e => logger.error(s"[T${taskId.value}] Error while marking task as completed", e))
    }.unit
  }

  private class StreamLogsThread(taskId: TaskId, runId: RunId, nodeName: String) extends Thread("executor/logs") {
    override def run(): Unit = {
      logger.debug(s"[T${taskId.value}] Logs thread started")
      trySleep(Duration.fromSeconds(2))
      while (!stopped.get()) {
        // Do not log anything here, otherwise it will create a logging loop, even if nothing else is printed.
        // We want to send only meaningful logs.
        val logs = extractLogs(stdoutBytes, "stdout") ++ extractLogs(stderrBytes, "stderr")
        if (logs.nonEmpty) {
          val f = Await.result(client.streamLogs(StreamLogsRequest(logs)).liftToTry)
          f match {
            case Return(_) => trySleep(Duration.fromSeconds(5))
            case Throw(InvalidTaskException()) => // Stop sending logs, task is now invalid.
            case Throw(e) => logger.error(s"[T${taskId.value}] Error while sending logs", e)
          }
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

  private class HeartbeatThread(taskId: TaskId) extends Thread("executor/heartbeat") {
    override def run(): Unit = {
      logger.debug(s"[T${taskId.value}] Heartbeat thread started")
      trySleep(Duration.fromSeconds(5))
      while (!stopped.get()) {
        // Do not log anything here, if communication with agent is broken it will be eventually detected on the
        // agent side. We want to send only meaningful logs.
        val f = Await.result(client.heartbeat(HeartbeatRequest(taskId)).liftToTry)
        f match {
          case Return(_) => trySleep(Duration.fromSeconds(15))
          case Throw(InvalidTaskException()) => // Stop sending logs, task is now invalid.
          case Throw(e) => logger.error(s"[T${taskId.value}] Error while heartbeat logs", e)
        }
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