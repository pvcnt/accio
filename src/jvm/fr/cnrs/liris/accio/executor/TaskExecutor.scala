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

import com.google.inject.Inject
import com.twitter.util._
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.framework.{OpExecutor, OpExecutorOpts, OutErr}
import fr.cnrs.liris.accio.core.util.{InfiniteLoopThreadLike, ThreadManager, WorkerPool}

/**
 * Execute tasks. Starting the execution of a task only requires a task identifier. The task executor will then
 * get the payload from the worker, execute the operator (through an [[OpExecutor]]) and finally report
 * the result to the worker. It also regularly send heartbeat and logs to the aforementioned worker. Communication
 * only happen between the executor and its local worker (on the same host).
 *
 * Contract: This class is designed for a single usage, i.e., a single call to [[execute]].
 *
 * @param opExecutor Operator executor.
 * @param client     Worker service client.
 * @param pool       Interruptible worker pool.
 */
final class TaskExecutor @Inject()(
  opExecutor: OpExecutor,
  client: AgentService$FinagleClient,
  @WorkerPool pool: FuturePool) extends StrictLogging {

  private[this] val executorId = ExecutorId(UUID.randomUUID().toString)
  private[this] val threads = new ThreadManager(pool)

  /**
   * Start the execution of a task, from its identifier. Payload will be downloaded, before the task
   * is actually started.
   *
   * Contract: This method should only be called once per instance.
   *
   * @param taskId Task identifier.
   * @param outErr Handle to stdout/stderr.
   * @return A future that will be completed once the task is completed. It always resolves as successful.
   */
  def submit(taskId: TaskId, outErr: OutErr): Future[Unit] = {
    logger.info(s"Starting execution of task ${taskId.value}")
    client
      .startExecutor(StartExecutorRequest(executorId, taskId))
      .transform {
        case Throw(e) =>
          logger.error(s"Error while starting task ${taskId.value}", e)
          Future.Done
        case Return(resp) => submit(taskId, resp.runId, resp.nodeName, resp.payload, outErr)
      }
  }

  /**
   * Start the execution of a task with its payload.
   *
   * @param taskId   Task identifier.
   * @param runId    Run identifier this task is part of.
   * @param nodeName Name of the node to execute.
   * @param payload  Payload to execute.
   * @param outErr   Handle to stdout/stderr.
   * @return A future that will be completed once the task is completed. It always resolves as successful.
   */
  private def submit(taskId: TaskId, runId: RunId, nodeName: String, payload: OpPayload, outErr: OutErr): Future[Unit] = {
    threads.submit(new HeartbeatThread(taskId))
    threads.submit(new StreamLogsThread(taskId, runId, nodeName, outErr))
    pool(execute(taskId, payload))
      .transform {
        case Throw(e) =>
          // Normally, operator executor is supposed to be robust enough to catch all errors. But we still handle
          // and uncaught error here, just in case...
          threads.killAll()
          logger.error(s"Operator raised an unexpected error", e)
          Future.value(OpResult(-999, Some(Errors.create(e))))
        case Return(result) =>
          //TODO: we should drain logs before killing the thread.
          threads.killAll()
          client
            .stopExecutor(StopExecutorRequest(executorId, taskId, result))
            .rescue { case e: Throwable =>
              logger.error(s"Error while marking task ${taskId.value} as completed", e)
              Future.Done
            }
      }
      .unit
  }

  /**
   * Execute a payload.
   *
   * @param taskId  Task identifier.
   * @param payload Payload to execute.
   * @return Result of the operator execution.
   */
  private def execute(taskId: TaskId, payload: OpPayload): OpResult = {
    val opts = OpExecutorOpts(useProfiler = true)
    opExecutor.execute(payload, opts)
  }

  /**
   * Thread sending collected stdout/stderr logs regularly.
   *
   * @param taskId   Task identifier.
   * @param runId    Run identifier this task is part of.
   * @param nodeName Name of the node being executed.
   * @param outErr   Handle to stdout/stderr.
   */
  private class StreamLogsThread(taskId: TaskId, runId: RunId, nodeName: String, outErr: OutErr)
    extends InfiniteLoopThreadLike {

    override protected def singleOperation(): Unit = {
      // Do not log anything in this method, to avoid creating an infinite logging loop if the communication with
      // the agent is broken.
      val logs = extractLogs(outErr)
      if (logs.nonEmpty) {
        // Only send logs if there is actually something to send...
        val f = client.streamExecutorLogs(StreamExecutorLogsRequest(executorId, taskId, logs))
        Await.result(f.liftToTry) match {
          case Return(_) => sleep(Duration.fromSeconds(5))
          case Throw(_: InvalidTaskException) => kill()
          case Throw(_: InvalidExecutorException) => kill()
          case Throw(_) => // Do nothing, hope it will go better on next try.
        }
      } else {
        sleep(Duration.fromSeconds(2))
      }
    }

    private def extractLogs(outErr: OutErr): Seq[RunLog] =
      extractLogs(outErr.stdoutAsString, "stdout") ++ extractLogs(outErr.stderrAsString, "stderr")

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

  /**
   * Thread sending a heartbeat regularly.
   *
   * @param taskId Task identifier.
   */
  private class HeartbeatThread(taskId: TaskId) extends InfiniteLoopThreadLike {
    override protected def singleOperation(): Unit = {
      // Do not log anything in this method, to avoid creating an infinite logging loop if the communication with
      // the agent is broken.
      val f = client.heartbeatExecutor(HeartbeatExecutorRequest(executorId))
      Await.result(f.liftToTry) match {
        case Return(_) => sleep(Duration.fromSeconds(15))
        case Throw(_: InvalidTaskException) => kill()
        case Throw(_: InvalidExecutorException) => kill()
        case Throw(_) => // Do nothing, hope it will go better on next try.
      }
    }
  }

}