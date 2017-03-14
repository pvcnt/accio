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

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import com.twitter.util._
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.framework.{OpExecutor, OpExecutorOpts, OutErr}
import fr.cnrs.liris.accio.core.util.WorkerPool

import scala.collection.mutable
import scala.util.control.NonFatal

/**
 * Execute tasks. Triggering the execution of a task only requires a task identifier. The task executor is then in
 * charge of getting the payload from the agent, executing the operator, thanks to an [[OpExecutor]] and finally
 * reporting the result to the agent. It also regularly send heartbeat and logs to the aforementioned agent.
 *
 * @param opExecutor Operator executor.
 * @param client     Worker service client.
 * @param pool       Interruptible worker pool.
 */
@Singleton
final class TaskExecutor @Inject()(
  opExecutor: OpExecutor,
  client: AgentService$FinagleClient,
  @WorkerPool pool: FuturePool) extends StrictLogging {

  private[this] val executorId = ExecutorId(UUID.randomUUID().toString)
  private[this] val futures = mutable.Set.empty[Future[_]]

  /**
   * Trigger the execution of a task.
   *
   * @param taskId Task identifier.
   * @return A future that will be completed once the task is completed. It never returns as an error.
   */
  def execute(taskId: TaskId, outErr: OutErr): Future[Unit] = synchronized {
    logger.info(s"Starting execution of task ${taskId.value}")
    client.startExecutor(StartExecutorRequest(executorId, taskId)).transform {
      case Throw(InvalidTaskException()) =>
        logger.error(s"Invalid task")
        Future.Done
      case Throw(e) =>
        logger.error(s"Error while starting task", e)
        Future.Done
      case Return(resp) => start(taskId, resp.runId, resp.nodeName, resp.payload, outErr)
    }
  }

  /**
   * Close the task executor.
   */
  def close(): Unit = synchronized {
    futures.foreach(_.raise(new FutureCancelledException))
  }

  private def start(taskId: TaskId, runId: RunId, nodeName: String, payload: OpPayload, outErr: OutErr): Future[Unit] = {
    futures += pool(new HeartbeatThread(taskId).run())
    futures += pool(new StreamLogsThread(taskId, runId, nodeName, outErr).run())
    val mainFuture = pool(new MainThread(taskId, payload).run())
      .handle {
        case NonFatal(e) =>
          logger.error(s"Operator raised an unexpected error", e)
          OpResult(-999, Some(Errors.create(e)))
      }.flatMap { result =>
      client.stopExecutor(StopExecutorRequest(executorId, taskId, result))
        .onFailure(e => logger.error(s"Error while marking task as completed", e))
    }.unit
    futures += mainFuture
    mainFuture
  }

  private class MainThread(taskId: TaskId, payload: OpPayload) {
    def run(): OpResult = {
      val opts = OpExecutorOpts(useProfiler = true)
      opExecutor.execute(payload, opts)
    }
  }

  private class StreamLogsThread(taskId: TaskId, runId: RunId, nodeName: String, outErr: OutErr) {
    def run(): Unit = {
      logger.debug(s"Logs thread started")
      sleep(Duration.fromSeconds(2))
      while (true) {
        // Do not log anything here, otherwise it will create a logging loop, even if nothing else is printed.
        // We want to send only meaningful logs.
        val logs = extractLogs(outErr.stdoutAsString, "stdout") ++ extractLogs(outErr.stderrAsString, "stderr")
        if (logs.nonEmpty) {
          val f = Await.result(client.streamExecutorLogs(StreamExecutorLogsRequest(executorId, taskId, logs)).liftToTry)
          f match {
            case Return(_) => sleep(Duration.fromSeconds(5))
            case Throw(InvalidTaskException()) =>
              // Stop the executor, task is now invalid.
              close()
            case Throw(e) => logger.error(s"Error while sending logs", e)
          }
        } else {
          sleep(Duration.fromSeconds(2))
        }
      }
    }

    private def extractLogs(content: String, classifier: String) = {
      val at = System.currentTimeMillis
      content.trim
        .split("\n")
        .toSeq
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(line => RunLog(runId, nodeName, at, classifier, line))
    }
  }

  private class HeartbeatThread(taskId: TaskId) {
    def run(): Unit = {
      logger.debug(s"Heartbeat thread started")
      sleep(Duration.fromSeconds(5))
      while (true) {
        // Do not log anything here, if communication with agent is broken it will be eventually detected on the
        // agent side. We want to send only meaningful logs.
        val f = Await.result(client.heartbeatExecutor(HeartbeatExecutorRequest(executorId)).liftToTry)
        f match {
          case Return(_) => sleep(Duration.fromSeconds(15))
          case Throw(InvalidTaskException()) =>
            // Stop the executor, task is now invalid.
            close()
          case Throw(e) => logger.error(s"Error while sending heartbeat", e)
        }
      }
    }
  }

  private def sleep(duration: Duration) = {
    try {
      Thread.sleep(duration.inMillis)
    } catch {
      case _: InterruptedException => // Do nothing.
    }
  }

}