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

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain._

/**
 * Coordinate between various services to manage lifecycle of runs, from initial launch to completion. All methods
 * alter run instances but never persist them.
 *
 * @param scheduler          Scheduler.
 * @param stateManager       State manager.
 * @param opRegistry         Operator registry.
 * @param graphFactory       Graph factory.
 * @param workflowRepository Workflow repository (read-only).
 * @param runRepository      Run repository (read-only).
 */
class RunLifecycleManager @Inject()(
  scheduler: Scheduler,
  stateManager: StateManager,
  opRegistry: OpRegistry,
  graphFactory: GraphFactory,
  workflowRepository: ReadOnlyWorkflowRepository,
  runRepository: ReadOnlyRunRepository)
  extends StrictLogging {

  /**
   * Launch a list of runs. Ready nodes of those runs will be submitted to the scheduler, and state of all runs
   * will updated to reflect this.
   *
   * @param runs Runs to launch.
   * @return Runs with updated state.
   */
  def launch(runs: Seq[Run]): Seq[Run] = {
    if (runs.isEmpty) {
      runs
    } else {
      // Workflow does exist, because it has been validated when creating the runs.
      val workflow = workflowRepository.get(runs.head.pkg.workflowId, runs.head.pkg.workflowVersion).get
      val rootNodes = graphFactory.create(workflow.graph).roots
      runs.map { run =>
        if (run.children.nonEmpty) {
          // Only mark a parent run as started, nothing has to be actually scheduled.
          run.copy(state = run.state.copy(status = RunStatus.Running, startedAt = Some(System.currentTimeMillis())))
        } else {
          // Submit root nodes of child runs to the scheduler.
          var newRun = run
          rootNodes.foreach { node =>
            newRun = schedule(newRun, node)
          }
          newRun
        }
      }
    }
  }

  /**
   * Mark a node inside a run as failed. It will recursively cancel all successors of this node. Execution of other
   * branches of the graph will however continue.
   *
   * @param run      Run.
   * @param nodeName Node that has failed, as part of the run.
   * @param result   Node result.
   * @return Run with updated state.
   */
  def onFailed(run: Run, nodeName: String, result: OpResult): Run = {
    val nodeState = run.state.nodes.find(_.nodeName == nodeName).get
    if (nodeState.completedAt.isDefined) {
      // On race requests, node state could be already marked as completed.
      run
    } else {
      val newNodeState = nodeState.copy(
        completedAt = Some(System.currentTimeMillis()),
        status = NodeStatus.Failed,
        result = Some(result))
      var newRun = replace(run, nodeState, newNodeState)
      newRun = cancelNextNodes(run, nodeName)
      updateProgress(newRun)
    }
  }

  /**
   * Mark a node inside a run as lost. It will recursively cancel all successors of this node. Execution of other
   * branches of the graph will however continue.
   *
   * @param run      Run.
   * @param nodeName Node that has failed, as part of the run.
   * @return Run with updated state.
   */
  def onLost(run: Run, nodeName: String): Run = {
    val nodeState = run.state.nodes.find(_.nodeName == nodeName).get
    if (nodeState.completedAt.isDefined) {
      // On race requests, node state could be already marked as completed.
      run
    } else {
      val newNodeState = nodeState.copy(completedAt = Some(System.currentTimeMillis()), status = NodeStatus.Lost)
      var newRun = replace(run, nodeState, newNodeState)
      newRun = cancelNextNodes(run, nodeName)
      updateProgress(newRun)
    }
  }

  /**
   * Mark a node inside a run as successful. It will schedule direct successors that are ready to be executed (i.e.,
   * whose all dependent nodes have been successfully completed).
   *
   * @param run      Run.
   * @param nodeName Node that completed, as part of the run.
   * @param result   Node result.
   * @return Run with updated state.
   */
  def onSuccess(run: Run, nodeName: String, result: OpResult, cacheKey: Option[CacheKey]): Run = {
    val nodeState = run.state.nodes.find(_.nodeName == nodeName).get
    if (nodeState.completedAt.isDefined) {
      // On race requests, node state could be already marked as completed.
      run
    } else {
      val newNodeState = nodeState.copy(
        completedAt = Some(System.currentTimeMillis()),
        status = NodeStatus.Success,
        result = Some(result),
        cacheKey = cacheKey)
      var newRun = replace(run, nodeState, newNodeState)
      newRun = scheduleNextNodes(newRun, nodeName)
      updateProgress(newRun)
    }
  }

  private def replace(run: Run, nodeState: NodeState, newNodeState: NodeState) = {
    run.copy(state = run.state.copy(nodes = run.state.nodes - nodeState + newNodeState))
  }

  /**
   * Submit a node to the scheduler.
   *
   * @param run  Run. Must contain the latest execution state, to allow the node to fetch its dependencies.
   * @param node Node to execute, as part of the run.
   * @return Updated run, after submission (not saved).
   */
  private def schedule(run: Run, node: Node): Run = {
    val opDef = opRegistry(node.op)
    val payload = createPayload(run, node, opDef)

    val now = System.currentTimeMillis()
    val nodeState = run.state.nodes.find(_.nodeName == node.name).get
    runRepository.get(payload.cacheKey) match {
      case Some(result) =>
        var newRun = replace(run, nodeState, nodeState.copy(
          startedAt = Some(now),
          completedAt = Some(now),
          status = NodeStatus.Success,
          cacheKey = Some(payload.cacheKey),
          result = Some(result.copy(cacheHit = true))))
        logger.debug(s"Cache hit. Run: ${run.id.value}, node: ${node.name}.")

        // Schedule next nodes.
        newRun = scheduleNextNodes(newRun, node.name)
        newRun
      case None =>
        val taskId = TaskId(UUID.randomUUID().toString)
        val job = Job(taskId, run.id, node.name, payload, opDef.resource)
        val key = scheduler.submit(job)
        val task = Task(
          id = taskId,
          runId = run.id,
          key = key,
          payload = payload,
          nodeName = node.name,
          createdAt = now,
          state = TaskState(TaskStatus.Scheduled))
        stateManager.save(task)
        logger.debug(s"Scheduled task ${task.id.value}. Run: ${run.id.value}, node: ${node.name}, op: ${payload.op}")
        replace(run, nodeState, nodeState.copy(status = NodeStatus.Scheduled))
    }
  }

  /**
   * Create the payload for a given node, by resolving the inputs.
   *
   * @param run   Run.
   * @param node  Node to execute, as part of the run.
   * @param opDef Operator definition for the node.
   */
  private def createPayload(run: Run, node: Node, opDef: OpDef): OpPayload = {
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
    val cacheKey = generateCacheKey(opDef, inputs, run.seed)
    OpPayload(node.op, run.seed, inputs, cacheKey)
  }

  private def updateProgress(run: Run): Run = {
    // Run could already be marked as completed if it was killed. In this case we do not want to update its state.
    // Otherwise, we check if this node was the last one to complete the run.
    if (run.state.completedAt.isEmpty) {
      // If all nodes are completed (not necessarily successfully), mark the run as completed. It is successfully
      // completed if all nodes completed successfully.
      if (run.state.nodes.forall(s => Utils.isCompleted(s.status))) {
        val newRunStatus = if (run.state.nodes.forall(_.status == NodeStatus.Success)) {
          RunStatus.Success
        } else {
          RunStatus.Failed
        }
        run.copy(state = run.state.copy(progress = 1, status = newRunStatus, completedAt = Some(System.currentTimeMillis())))
      } else {
        // Run is not yet completed, only, update progress.
        val progress = run.state.nodes.count(s => Utils.isCompleted(s.status)).toDouble / run.state.nodes.size
        run.copy(state = run.state.copy(progress = progress))
      }
    } else {
      run
    }
    //TODO: update parent run if any.
  }

  /**
   * Generate a unique cache key for the outputs of a node. It is based on operator definition, inputs and seed.
   *
   * @param opDef  Operator definition.
   * @param inputs Node inputs.
   * @param seed   Seed for unstable operators.
   */
  private def generateCacheKey(opDef: OpDef, inputs: Map[String, Value], seed: Long): CacheKey = {
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

  private def scheduleNextNodes(run: Run, nodeName: String): Run = {
    val graph = getGraph(run)
    val nextNodes = getNextNodes(run, graph, nodeName).filter { nextNode =>
      nextNode.predecessors.forall { dep =>
        run.state.nodes.find(_.nodeName == dep).get.status == NodeStatus.Success
      }
    }
    var newRun = run
    nextNodes.foreach { nextNode =>
      newRun = schedule(newRun, nextNode)
    }
    newRun
  }

  private def cancelNextNodes(run: Run, nodeName: String): Run = {
    val graph = getGraph(run)
    val nextNodes = getNextNodes(run, graph, nodeName).flatMap(node => Seq(node) ++ getNextNodes(run, graph, node.name))
    var newRun = run
    nextNodes.foreach { nextNode =>
      val nodeState = newRun.state.nodes.find(_.nodeName == nextNode.name).get
      val newNodeState = nodeState.copy(completedAt = Some(System.currentTimeMillis()), status = NodeStatus.Cancelled)
      newRun = replace(newRun, nodeState, newNodeState)
    }
    newRun
  }

  private def getNextNodes(run: Run, graph: Graph, nodeName: String): Set[Node] = {
    graph(nodeName).successors
      .map(graph.apply)
      .filter { nextNode =>
        run.state.nodes.find(_.nodeName == nextNode.name).get.status == NodeStatus.Waiting
      }
  }

  private def getGraph(run: Run): Graph = {
    // Workflow does exist, because it has been validate when creating the runs.
    val workflow = workflowRepository.get(run.pkg.workflowId, run.pkg.workflowVersion).get
    graphFactory.create(workflow.graph)
  }
}