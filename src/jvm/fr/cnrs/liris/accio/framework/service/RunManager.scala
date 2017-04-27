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

package fr.cnrs.liris.accio.framework.service

import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.framework.api
import fr.cnrs.liris.accio.framework.api.Utils
import fr.cnrs.liris.accio.framework.api.thrift._
import fr.cnrs.liris.accio.framework.storage.{RunQuery, Storage}
import fr.cnrs.liris.common.util.cache.CacheBuilder

/**
 * Provider a high-level service to manage runs.
 *
 * @param schedulerService Scheduler service.
 * @param graphFactory     Graph factory.
 * @param storage          Storage.
 */
@Singleton
final class RunManager @Inject()(schedulerService: SchedulerService, graphFactory: GraphFactory, storage: Storage)
  extends StrictLogging {

  private[this] val graphs = CacheBuilder().maximumSize(25).build((pkg: Package) => {
    // Workflow does exist, because it has been validate when creating the runs.
    val workflow = storage.workflows.get(pkg.workflowId, pkg.workflowVersion).get
    graphFactory.create(workflow.graph)
  })

  /**
   * Launch a run. Ready nodes of those runs will be submitted to the scheduler.
   *
   * @param run    Run to launch.
   * @param parent Parent run, if any.
   * @return Updated run and parent run.
   */
  def launch(run: Run, parent: Option[Run]): (Run, Option[Run]) = {
    if (run.children.nonEmpty) {
      // A parent run is never launched, only mark it as started.
      (run.copy(state = run.state.copy(startedAt = Some(System.currentTimeMillis()), status = RunStatus.Running)), parent)
    } else {
      // Submit root nodes of child runs to the scheduler.
      val rootNodes = graphs(run.pkg).roots
      val newRun = schedule(run, rootNodes)
      updateProgress(newRun, parent)
    }
  }

  /**
   * Mark a list of nodes as killed.
   *
   * @param run       Run.
   * @param nodeNames Nodes that have been killed, as part of the run.
   * @param parent    Parent run, if any.
   * @return Updated run and parent run.
   */
  def onKill(run: Run, nodeNames: Set[String], parent: Option[Run]): (Run, Option[Run]) = {
    var newRun = run
    nodeNames.foreach { nodeName =>
      val nodeState = run.state.nodes.find(_.name == nodeName).get
      if (nodeState.completedAt.isDefined) {
        // On race requests, node state could be already marked as completed.
        (run, parent)
      } else {
        val newNodeState = nodeState.copy(completedAt = Some(System.currentTimeMillis()), status = NodeStatus.Killed)
        newRun = replace(run, nodeState, newNodeState)
      }
    }
    newRun = cancelAllNodes(newRun)
    updateProgress(newRun, parent)
  }

  /**
   * Mark a node inside a run as started.
   *
   * @param run      Run.
   * @param nodeName Node that started, as part of the run.
   * @return Updated run.
   */
  def onStart(run: Run, nodeName: String): Run = {
    val nodeState = run.state.nodes.find(_.name == nodeName).get
    if (nodeState.startedAt.nonEmpty) {
      // Node state could be already marked as started if another task was already spawned for this node.
      run
    } else {
      // Mark node as started.
      val now = System.currentTimeMillis()
      val newNodeState = nodeState.copy(startedAt = Some(now), status = NodeStatus.Running)
      var newRun = replace(run, nodeState, newNodeState)
      // Mark run as started, if not already.
      if (newRun.state.startedAt.isEmpty) {
        newRun = newRun.copy(state = newRun.state.copy(startedAt = Some(now), status = RunStatus.Running))
      }
      newRun
    }
  }

  /**
   * Mark a node inside a run as failed. It will recursively cancel all successors of this node. Execution of other
   * branches of the graph will however continue.
   *
   * @param run      Run.
   * @param nodeName Node that has failed, as part of the run.
   * @param result   Node result.
   * @param parent   Parent run, if any.
   * @return Updated run and parent run.
   */
  def onFailed(run: Run, nodeName: String, result: OpResult, parent: Option[Run]): (Run, Option[Run]) = {
    val nodeState = run.state.nodes.find(_.name == nodeName).get
    if (nodeState.completedAt.isDefined) {
      // On race requests, node state could be already marked as completed.
      (run, parent)
    } else {
      val newNodeState = nodeState.copy(
        completedAt = Some(System.currentTimeMillis()),
        status = NodeStatus.Failed,
        result = Some(result))
      var newRun = replace(run, nodeState, newNodeState)
      newRun = cancelNextNodes(newRun, nodeName)
      updateProgress(newRun, parent)
    }
  }

  /**
   * Mark a node inside a run as lost. It will recursively cancel all successors of this node. Execution of other
   * branches of the graph will however continue.
   *
   * @param run      Run.
   * @param nodeName Node that has failed, as part of the run.
   * @param parent   Parent run, if any.
   * @return Updated run and parent run.
   */
  def onLost(run: Run, nodeName: String, parent: Option[Run]): (Run, Option[Run]) = {
    val nodeState = run.state.nodes.find(_.name == nodeName).get
    if (nodeState.completedAt.isDefined) {
      // On race requests, node state could be already marked as completed.
      (run, parent)
    } else {
      val newNodeState = nodeState.copy(completedAt = Some(System.currentTimeMillis()), status = NodeStatus.Lost)
      var newRun = replace(run, nodeState, newNodeState)
      newRun = cancelNextNodes(newRun, nodeName)
      updateProgress(newRun, parent)
    }
  }

  /**
   * Mark a node inside a run as successful. It will schedule direct successors that are ready to be executed (i.e.,
   * whose all dependent nodes have been successfully completed).
   *
   * @param run      Run.
   * @param nodeName Node that completed, as part of the run.
   * @param result   Node result.
   * @param parent   Parent run, if any.
   * @return Updated run and parent run.
   */
  def onSuccess(run: Run, nodeName: String, result: OpResult, cacheKey: Option[CacheKey], parent: Option[Run]): (Run, Option[Run]) = {
    val nodeState = run.state.nodes.find(_.name == nodeName).get
    if (nodeState.completedAt.isDefined) {
      // On race requests, node state could be already marked as completed.
      (run, parent)
    } else {
      val newNodeState = nodeState.copy(
        completedAt = Some(System.currentTimeMillis()),
        status = NodeStatus.Success,
        result = Some(result),
        cacheKey = cacheKey)
      var newRun = replace(run, nodeState, newNodeState)
      newRun = scheduleNextNodes(newRun, nodeName)
      updateProgress(newRun, parent)
    }
  }

  private def replace(run: Run, nodeState: NodeState, newNodeState: NodeState) = {
    run.copy(state = run.state.copy(nodes = run.state.nodes - nodeState + newNodeState))
  }

  private def schedule(run: Run, nodes: Set[api.Node]): Run = {
    var newRun = run
    nodes.foreach { node =>
      newRun = schedule(newRun, node)
    }
    newRun
  }

  /**
   * Submit a node to the scheduler.
   *
   * @param run  Run. Must contain the latest execution state, to allow the node to fetch its dependencies.
   * @param node Node to execute, as part of the run.
   * @return Updated run.
   */
  private def schedule(run: Run, node: api.Node): Run = {
    val nodeState = run.state.nodes.find(_.name == node.name).get
    val maybeResult = schedulerService.submit(run, node)
    maybeResult match {
      case Some((cacheKey, result)) =>
        // Save node result.
        val now = System.currentTimeMillis()
        var newRun = replace(run, nodeState, nodeState.copy(
          startedAt = Some(now),
          completedAt = Some(now),
          status = NodeStatus.Success,
          cacheKey = Some(cacheKey),
          cacheHit = true,
          result = Some(result)))

        // Mark run as started, if not already.
        if (newRun.state.startedAt.isEmpty) {
          newRun = newRun.copy(state = newRun.state.copy(startedAt = Some(now), status = RunStatus.Running))
        }

        // Schedule next nodes.
        scheduleNextNodes(newRun, node.name)
      case None =>
        // Node has been scheduled.
        replace(run, nodeState, nodeState.copy(status = NodeStatus.Scheduled))
    }
  }

  private def updateProgress(run: Run, parent: Option[Run]): (Run, Option[Run]) = {
    val now = System.currentTimeMillis()
    var newRun = run
    if (newRun.state.completedAt.isEmpty) {
      if (newRun.state.nodes.forall(s => Utils.isCompleted(s.status))) {
        // Mark run as completed, if all nodes are completed. It is successful if all nodes were successful.
        val newRunStatus = if (newRun.state.nodes.forall(_.status == NodeStatus.Success)) {
          RunStatus.Success
        } else if (newRun.state.nodes.exists(_.status == NodeStatus.Killed)) {
          RunStatus.Killed
        } else {
          RunStatus.Failed
        }
        newRun = newRun.copy(state = newRun.state.copy(progress = 1, status = newRunStatus, completedAt = Some(now)))
      } else {
        // Run is not yet completed, only update progress.
        val progress = newRun.state.nodes.count(s => Utils.isCompleted(s.status)).toDouble / newRun.state.nodes.size
        newRun = newRun.copy(state = newRun.state.copy(progress = progress))
      }
    }

    (newRun, updateParentProgress(newRun, parent))
  }

  private def updateParentProgress(run: Run, parent: Option[Run]): Option[Run] = {
    parent.map { parent =>
      if (parent.state.completedAt.isEmpty) {
        val siblings = storage.runs.find(RunQuery(parent = Some(parent.id))).results.filter(_.id != run.id) ++ Seq(run)
        if (siblings.forall(s => Utils.isCompleted(s.state.status))) {
          // Mark parent run as completed if all children are completed. It is successful if all runs were successful.
          val newRunState = if (siblings.forall(_.state.status == RunStatus.Success)) {
            RunStatus.Success
          } else if (siblings.exists(_.state.status == RunStatus.Killed)) {
            RunStatus.Killed
          } else {
            RunStatus.Failed
          }
          parent.copy(state = parent.state.copy(progress = 1, status = newRunState, completedAt = Some(System.currentTimeMillis())))
        } else {
          // Parent is not yet completed, only update progress.
          val progress = siblings.map(_.state.progress).sum / parent.children.size
          parent.copy(state = parent.state.copy(progress = progress))
        }
      } else {
        parent
      }
    }
  }

  private def scheduleNextNodes(run: Run, nodeName: String): Run = {
    val graph = graphs(run.pkg)
    val nextNodes = getNextNodes(run, graph, nodeName).filter { nextNode =>
      nextNode.predecessors.forall { dep =>
        run.state.nodes.find(_.name == dep).get.status == NodeStatus.Success
      }
    }
    schedule(run, nextNodes)
  }

  private def cancelNextNodes(run: Run, nodeName: String): Run = {
    val graph = graphs(run.pkg)
    val nextNodes = getNextNodes(run, graph, nodeName).flatMap(node => Seq(node) ++ getNextNodes(run, graph, node.name))
    var newRun = run
    nextNodes.foreach { nextNode =>
      val nodeState = newRun.state.nodes.find(_.name == nextNode.name).get
      val newNodeState = nodeState.copy(completedAt = Some(System.currentTimeMillis()), status = NodeStatus.Cancelled)
      newRun = replace(newRun, nodeState, newNodeState)
    }
    newRun
  }

  private def cancelAllNodes(run: Run): Run = {
    val graph = graphs(run.pkg)
    var newRun = run
    graph.nodes.foreach { node =>
      val nodeState = newRun.state.nodes.find(_.name == node.name).get
      if (!Utils.isCompleted(nodeState.status)) {
        val newNodeState = nodeState.copy(completedAt = Some(System.currentTimeMillis()), status = NodeStatus.Cancelled)
        newRun = replace(newRun, nodeState, newNodeState)
      }
    }
    newRun
  }

  private def getNextNodes(run: Run, graph: api.Graph, nodeName: String): Set[api.Node] = {
    graph(nodeName).successors
      .map(graph.apply)
      .filter { nextNode =>
        run.state.nodes.find(_.name == nextNode.name).get.status == NodeStatus.Waiting
      }
  }
}