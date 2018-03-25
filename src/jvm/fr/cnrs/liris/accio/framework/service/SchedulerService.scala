/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.framework.service

import java.util.UUID

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.framework.api
import fr.cnrs.liris.accio.framework.api.Input
import fr.cnrs.liris.accio.framework.api.thrift._
import fr.cnrs.liris.accio.framework.scheduler.{EventType, Scheduler}
import fr.cnrs.liris.accio.framework.storage.Storage

/**
 * Wrapper around the actual scheduler, handling task creation.
 *
 * @param scheduler  Scheduler.
 * @param opRegistry Operator registry.
 * @param storage    Storage.
 */
final class SchedulerService @Inject()(scheduler: Scheduler, opRegistry: OpRegistry, storage: Storage)
  extends StrictLogging {

  /**
   * Submit a node to the scheduler. If will first check if a cached result is available. If it is the case, the node
   * will not be actually scheduled and the result will be returned. Otherwise a job will be submitted to the
   * scheduler and nothing will be returned.
   *
   * @param run       Run. Must contain the latest execution state, to allow the node to fetch its dependencies.
   * @param node      Node to execute, as part of the run.
   * @param readCache Whether to allow to fetch a cached result from the cache.
   * @return Node result and cache key, if available.
   */
  def submit(run: Run, node: api.Node, readCache: Boolean = true): Option[(CacheKey, OpResult)] = {
    val opDef = opRegistry(node.op)
    val payload = createPayload(run, node, opDef)
    val maybeResult = if (readCache) storage.runs.get(payload.cacheKey) else None
    maybeResult match {
      case Some(result) =>
        logger.debug(s"Cache hit. Run: ${run.id.value}, node: ${node.name}.")
        Some(payload.cacheKey -> result)
      case None =>
        val taskId = TaskId(UUID.randomUUID().toString)
        val task = Task(taskId, run.id, node.name, payload, System.currentTimeMillis(), TaskState.Waiting, opDef.resource)
        scheduler.submit(task)
        scheduler.houseKeeping(EventType.LessResource)
        logger.debug(s"Scheduled task ${task.id.value}. Run: ${run.id.value}, node: ${node.name}, op: ${payload.op}")
        None
    }
  }

  /**
   * Create the payload for a given node, by resolving the inputs.
   *
   * @param run   Run.
   * @param node  Node to execute, as part of the run.
   * @param opDef Operator definition for the node.
   */
  private def createPayload(run: Run, node: api.Node, opDef: OpDef): OpPayload = {
    val inputs = node.inputs.flatMap { case (portName, input) =>
      val maybeValue = input match {
        case Input.Param(paramName) => run.params.get(paramName)
        case Input.Reference(ref) =>
          run.state.nodes.find(_.name == ref.node)
            .flatMap(node => node.result.flatMap(_.artifacts.find(_.name == ref.port)))
            .map(_.value)
        case Input.Constant(v) => Some(v)
      }
      maybeValue.map(value => portName -> value)
    }
    val cacheKey = generateCacheKey(opDef, inputs, run.seed)
    OpPayload(opDef.className, run.seed, inputs, cacheKey)
  }

  /**
   * Generate a unique cache key for the outputs of a node. It is based on operator definition, inputs and seed.
   *
   * @param opDef  Operator definition.
   * @param inputs Node inputs.
   * @param seed   Seed for unstable operators.
   */
  def generateCacheKey(opDef: OpDef, inputs: Map[String, Value], seed: Long): CacheKey = {
    val hasher = Hashing.sha1().newHasher()
    hasher.putString(opDef.name, Charsets.UTF_8)
    hasher.putLong(if (opDef.unstable) seed else 0L)
    opDef.inputs.map { argDef =>
      hasher.putString(argDef.name, Charsets.UTF_8)
      val value = inputs.get(argDef.name).orElse(argDef.defaultValue)
      hasher.putInt(value.hashCode)
    }
    CacheKey(hasher.hash().toString)
  }
}