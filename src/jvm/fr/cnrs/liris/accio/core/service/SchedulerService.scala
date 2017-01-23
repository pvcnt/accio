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

package fr.cnrs.liris.accio.core.service

import java.util.UUID

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain._

/**
 * Wrapper around a scheduler handling payload creation and task submission.
 *
 * @param scheduler    Scheduler.
 * @param stateManager State manager.
 * @param opRegistry   Operator registry.
 */
class SchedulerService @Inject()(scheduler: Scheduler, stateManager: StateManager, opRegistry: OpRegistry) extends StrictLogging {
  /**
   * Submit a node to the scheduler. It submits a job to the scheduler, creates a task from it and saves this task in
   * the state manager.
   *
   * @param run  Run. Must contain the latest execution state, to allow the node to fetch its dependencies.
   * @param node Node to execute, as part of the run.
   * @return Task that has been created.
   */
  def submit(run: Run, node: Node): Task = {
    val payload = createPayload(run, node)
    val taskId = TaskId(UUID.randomUUID().toString)
    val job = Job(taskId, run.id, node.name, payload, opRegistry(payload.op).resource)
    val key = scheduler.submit(job)
    val task = Task(
      id = taskId,
      runId = run.id,
      key = key,
      payload = payload,
      nodeName = node.name,
      createdAt = System.currentTimeMillis(),
      scheduler = scheduler.getClass.getName,
      state = TaskState(TaskStatus.Scheduled))
    stateManager.save(task)
    logger.debug(s"[T${task.id.value}] Scheduled task. Run: ${run.id.value}, node: ${node.name}, op: ${payload.op}")
    task
  }

  def kill(run: Run): Unit = {

  }

  /**
   * Create the payload for a given node, by resolving the inputs.
   *
   * @param run  Run.
   * @param node Node to execute, as part of the run.
   */
  private def createPayload(run: Run, node: Node) = {
    val inputs = node.inputs.map { case (portName, input) =>
      val value = input match {
        case ParamInput(paramName) => run.params(paramName)
        case ReferenceInput(ref) =>
          val maybeArtifact = run.state.nodes.find(_.nodeName == ref.node)
            .flatMap(node => node.result.flatMap(_.artifacts.find(_.name == ref.port)))
          maybeArtifact match {
            case None =>
              // Should never be there...
              throw new IllegalStateException(s"Artifact of ${ref.node}/${ref.port} in run ${run.id.value} is not available")
            case Some(artifact) => artifact.value
          }
        case ValueInput(v) => v
      }
      portName -> value
    }
    OpPayload(node.op, run.seed, inputs)
  }
}