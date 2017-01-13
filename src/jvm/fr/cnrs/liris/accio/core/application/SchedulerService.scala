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

package fr.cnrs.liris.accio.core.application

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.domain._

class SchedulerService @Inject()(scheduler: Scheduler, stateManager: StateManager) {
  def submit(run: Run, node: Node): Task = {
    val payload = createPayload(run, node)
    val task = scheduler.submit(run.id, node.name, payload)
    stateManager.save(task)
    task
  }

  private def createPayload(run: Run, node: Node) = {
    val inputs = node.inputs.map { case (portName, input) =>
      val value = input match {
        case ParamInput(paramName) => run.params(paramName)
        case ReferenceInput(ref) =>
          val maybeArtifact = run.state.nodes.find(_.nodeName == ref.node)
            .flatMap(node => node.result.get.artifacts.find(_.name == ref.port))
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