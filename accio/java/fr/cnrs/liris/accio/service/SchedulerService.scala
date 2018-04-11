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

package fr.cnrs.liris.accio.service

import java.util.UUID

import com.google.inject.Inject
import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.api
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.api.{Input, OpRegistry, Utils}
import fr.cnrs.liris.accio.scheduler.{Process, Scheduler}
import fr.cnrs.liris.accio.storage.Storage

/**
 * Wrapper around the actual scheduler, handling task creation.
 *
 * @param scheduler  Scheduler.
 * @param opRegistry Operator registry.
 * @param storage    Storage.
 */
final class SchedulerService @Inject()(scheduler: Scheduler, opRegistry: OpRegistry, storage: Storage)
  extends Logging {

  /**
   * Submit a node to the scheduler. If will first check if a cached result is available. If it is the case, the node
   * will not be actually scheduled and the result will be returned. Otherwise a job will be submitted to the
   * scheduler and nothing will be returned.
   *
   * @param run       Run. Must contain the latest execution state, to allow the node to fetch its dependencies.
   * @param node      Node to execute, as part of the run.
   * @param readCache Whether to allow to fetch a cached result from the cache.
   * @return Updated run.
   */
  def submit(run: Run, node: api.Node, readCache: Boolean = true): Run = {
    val nodeStatus = run.state.nodes.find(_.name == node.name).get
    val opDef = opRegistry(node.op)
    val payload = createPayload(run, node, opDef)
    val taskId = UUID.randomUUID().toString
    val process = Process(taskId, run.id, node.name, payload)
    scheduler.submit(process)

    logger.debug(s"Submitted process ${process.id}. Run: ${run.id}, node: ${node.name}, op: ${payload.op}")
    val newNodeStatus = nodeStatus.copy(status = TaskState.Scheduled, taskId = Some(taskId))
    run.copy(state = run.state.copy(nodes = run.state.nodes - nodeStatus + newNodeStatus))
  }

  def kill(run: Run): Run = {
    var newRun = run
    newRun.state
      .nodes
      .filter(node => !Utils.isCompleted(node.status) && node.taskId.isDefined)
      .foreach { node =>
        if (scheduler.kill(node.taskId.get)) {
          val newNodeStatus = node.copy(completedAt = Some(System.currentTimeMillis()), status = TaskState.Killed)
          newRun = newRun.copy(state = newRun.state.copy(nodes = newRun.state.nodes - node + newNodeStatus))
        }
      }
    newRun
  }

  /**
   * Create the payload for a given node, by resolving the inputs.
   *
   * @param run   Run.
   * @param node  Node to execute, as part of the run.
   * @param opDef Operator definition for the node.
   */
  private def createPayload(run: Run, node: api.Node, opDef: OpDef): OpPayload = {
    val inputs = node.inputs.toSeq.flatMap { case (portName, input) =>
      val maybeValue = input match {
        case Input.Param(paramName) => run.params.get(paramName)
        case Input.Reference(ref) =>
          run.state.nodes.find(_.name == ref.node)
            .flatMap(node => node.result.flatMap(_.artifacts.find(_.name == ref.port)))
            .map(_.value)
        case Input.Constant(v) => Some(v)
      }
      maybeValue.map(value => Artifact(portName, value))
    }
    OpPayload(opDef.name, run.seed, inputs, opDef.resource)
  }
}
