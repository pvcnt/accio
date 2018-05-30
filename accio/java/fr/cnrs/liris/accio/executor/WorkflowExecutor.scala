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

import java.nio.file.Path

import com.twitter.util.logging.Logging
import com.twitter.util.{Future, Promise}
import fr.cnrs.liris.accio.discovery.OpRegistry
import fr.cnrs.liris.accio.domain.{OpResult, Workflow}
import fr.cnrs.liris.lumos.domain.ExecStatus
import fr.cnrs.liris.lumos.transport.EventTransport

final class WorkflowExecutor(
  workflow: Workflow,
  workDir: Path,
  registry: OpRegistry,
  transport: EventTransport)
  extends Logging {

  private[this] val stateMachine = new StateMachine(workflow)
  private[this] val handler = new TaskLifecycleHandler {
    override def taskCompleted(name: String, exitCode: Int, result: OpResult): Unit = {
      applySideEffects(stateMachine.stepCompleted(name, exitCode, result))
    }

    override def taskStarted(name: String): Unit = applySideEffects(stateMachine.stepStarted(name))
  }
  private[this] val taskExecutor = new TaskExecutor(workDir, handler)
  private[this] val promise = new Promise[Int]

  def execute(): Future[Int] = {
    logger.info(s"Starting execution of workflow ${workflow.name}")
    applySideEffects(stateMachine.startJob())
    promise
  }

  def close(): Unit = taskExecutor.close()

  private def applySideEffects(sideEffects: Iterable[SideEffect]): Unit = {
    sideEffects.foreach {
      case SideEffect.Publish(event) => transport.sendEvent(event)
      case SideEffect.Kill(name) => taskExecutor.kill(name)
      case SideEffect.Schedule(stepName, payload) =>
        registry.get(payload.op) match {
          case None =>
            logger.error(s"Cannot schedule unknown operator: ${payload.op}")
            applySideEffects(stateMachine.cancelJob())
          case Some(op) => taskExecutor.submit(stepName, op.executable, payload)
        }
    }

    val state = stateMachine.currentState
    if (state.isCompleted) {
      logger.info(s"Completed execution of workflow ${workflow.name}")
      promise.setValue(if (state == ExecStatus.Successful) 0 else 1)
    }
  }
}