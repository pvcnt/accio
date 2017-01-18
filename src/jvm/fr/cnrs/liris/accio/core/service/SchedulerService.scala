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

import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain._

/**
 * Wrapper around a scheduler handling payload creation and task submission.
 *
 * @param scheduler    Scheduler.
 * @param stateManager State manager.
 */
class SchedulerService(scheduler: Scheduler, stateManager: StateManager) extends StrictLogging {
  /**
   * Submit a node to the scheduler.
   *
   * @param run  Run. Must contain the latest execution state, to allow the node to fetch its dependencies.
   * @param node Node to execute, as part of the run.
   * @return Task submitted to the scheduler.
   */
  def submit(run: Run, node: Node): Task = {
    val payload = createPayload(run, node)
    val task = scheduler.submit(run.id, node.name, payload)
    stateManager.save(task)
    logger.debug(s"[T${task.id.value}] Scheduled task. Run: ${run.id.value}, node: ${node.name}, op: ${payload.op}")
    task
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