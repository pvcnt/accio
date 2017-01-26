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

import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.common.util.cache.CacheBuilder

/**
 * Coordinate between various services to manage lifecycle of runs, from initial launch to completion. All methods
 * alter run instances but never persist them.
 *
 * @param schedulerService   Scheduler service.
 * @param graphFactory       Graph factory.
 * @param workflowRepository Workflow repository (read-only).
 */
@Singleton
final class RunLifecycleManager @Inject()(
  schedulerService: SchedulerService,
  graphFactory: GraphFactory,
  workflowRepository: ReadOnlyWorkflowRepository)
  extends StrictLogging {

  private[this] val graphs = CacheBuilder().maximumSize(50).build((pkg: Package) => {
    // Workflow does exist, because it has been validate when creating the runs.
    val workflow = workflowRepository.get(pkg.workflowId, pkg.workflowVersion).get
    graphFactory.create(workflow.graph)
  })

  /**
   * Launch a run. Ready nodes of those runs will be submitted to the scheduler.
   *
   * @param run Run to launch.
   * @return Updated run.
   */
  def launch(run: Run): Run = {
    // Workflow does exist, because it has been validated when creating the runs.
    val rootNodes = graphs(run.pkg).roots
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

  /**
   * Mark a node inside a run as started.
   *
   * @param run      Run.
   * @param nodeName Node that started, as part of the run.
   * @return Updated run.
   */
  def onStart(run: Run, nodeName: String): Run = {
    val nodeState = run.state.nodes.find(_.nodeName == nodeName).get
    if (nodeState.startedAt.nonEmpty) {
      // Node state could be already marked as started if another task was already spawned for this node.
      run
    } else {
      // Mark node as started.
      val now = System.currentTimeMillis()
      val newNodeState = nodeState.copy(startedAt = Some(now), status = NodeStatus.Running)
      var newRun = replace(run, nodeState, newNodeState)
      if (newRun.state.startedAt.isEmpty) {
        // Mark run as started, if not already.
        newRun = newRun.copy(state = newRun.state.copy(startedAt = Some(now), status = RunStatus.Running))
      }
      updateProgress(newRun)
    }
  }

  /**
   * Mark a node inside a run as failed. It will recursively cancel all successors of this node. Execution of other
   * branches of the graph will however continue.
   *
   * @param run      Run.
   * @param nodeName Node that has failed, as part of the run.
   * @param result   Node result.
   * @return Updated run.
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
      newRun = cancelNextNodes(newRun, nodeName)
      updateProgress(newRun)
    }
  }

  /**
   * Mark a node inside a run as lost. It will recursively cancel all successors of this node. Execution of other
   * branches of the graph will however continue.
   *
   * @param run      Run.
   * @param nodeName Node that has failed, as part of the run.
   * @return Updated run.
   */
  def onLost(run: Run, nodeName: String): Run = {
    val nodeState = run.state.nodes.find(_.nodeName == nodeName).get
    if (nodeState.completedAt.isDefined) {
      // On race requests, node state could be already marked as completed.
      run
    } else {
      val newNodeState = nodeState.copy(completedAt = Some(System.currentTimeMillis()), status = NodeStatus.Lost)
      var newRun = replace(run, nodeState, newNodeState)
      newRun = cancelNextNodes(newRun, nodeName)
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
   * @return Updated run.
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
   * @return Updated run.
   */
  private def schedule(run: Run, node: Node): Run = {
    val nodeState = run.state.nodes.find(_.nodeName == node.name).get
    val maybeResult = schedulerService.submit(run, node)
    maybeResult match {
      case Some((cacheKey, result)) =>
        // Save node result.
        val now = System.currentTimeMillis()
        val newRun = replace(run, nodeState, nodeState.copy(
          startedAt = Some(now),
          completedAt = Some(now),
          status = NodeStatus.Success,
          cacheKey = Some(cacheKey),
          cacheHit = true,
          result = Some(result)))

        // Schedule next nodes.
        scheduleNextNodes(newRun, node.name)
      case None =>
        // Node has been scheduled.
        replace(run, nodeState, nodeState.copy(status = NodeStatus.Scheduled))
    }
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

  private def scheduleNextNodes(run: Run, nodeName: String): Run = {
    val graph = graphs(run.pkg)
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
    val graph = graphs(run.pkg)
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
}