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

import com.google.inject.{Inject, Singleton}
import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.api.thrift.{Run, TaskState}
import fr.cnrs.liris.accio.api.{Graph, Node, Utils, thrift}
import fr.cnrs.liris.accio.storage.{RunQuery, Storage}
import fr.cnrs.liris.common.util.cache.CacheBuilder

/**
 * Provider a high-level service to manage runs.
 *
 * @param schedulerService Scheduler service.
 * @param graphFactory     Graph factory.
 * @param storage          Storage.
 */
@Singleton
final class RunManager @Inject()(schedulerService: SchedulerService, graphFactory: GraphValidator, storage: Storage)
  extends Logging {

  private[this] val graphs = CacheBuilder().maximumSize(25).build((pkg: thrift.Package) => {
    // Workflow does exist, because it has been validated when creating the runs, and workflows
    // cannot be deleted (via the public API at least...).
    val workflow = storage.read(_.workflows.get(pkg.workflowId, pkg.workflowVersion)).get
    Graph.fromThrift(workflow.graph)
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
      (run.copy(state = run.state.copy(startedAt = Some(System.currentTimeMillis()), status = TaskState.Running)), parent)
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
   * @param run    Run.
   * @param parent Parent run, if any.
   * @return Updated run and parent run.
   */
  def onKill(run: Run, parent: Option[Run]): (Run, Option[Run]) = {
    var newRun = run
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
    val nodeStatus = run.state.nodes.find(_.name == nodeName).get
    val now = System.currentTimeMillis()
    var newRun = run
    if (nodeStatus.startedAt.isEmpty) {
      // Node state could be already marked as started if another task was already spawned for this node.
      // Mark node as started.
      val newNodeStatus = nodeStatus.copy(startedAt = Some(now), status = TaskState.Running)
      newRun = replace(run, nodeStatus, newNodeStatus)
    }
    // Mark run as started, if not already.
    if (newRun.state.startedAt.isEmpty) {
      newRun = newRun.copy(state = newRun.state.copy(startedAt = Some(now), status = TaskState.Running))
    }
    newRun
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
  def onFailed(run: Run, nodeName: String, result: thrift.OpResult, parent: Option[Run]): (Run, Option[Run]) = {
    val nodeStatus = run.state.nodes.find(_.name == nodeName).get
    if (nodeStatus.completedAt.isDefined) {
      // On race requests, node state could be already marked as completed.
      (run, parent)
    } else {
      val newNodeStatus = nodeStatus.copy(
        completedAt = Some(System.currentTimeMillis()),
        status = TaskState.Failed,
        result = Some(result))
      var newRun = replace(run, nodeStatus, newNodeStatus)
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
  def onSuccess(run: Run, nodeName: String, result: thrift.OpResult, cacheKey: Option[String], parent: Option[Run]): (Run, Option[Run]) = {
    val NodeStatus = run.state.nodes.find(_.name == nodeName).get
    if (NodeStatus.completedAt.isDefined) {
      // On race requests, node state could be already marked as completed.
      (run, parent)
    } else {
      val newNodeStatus = NodeStatus.copy(
        completedAt = Some(System.currentTimeMillis()),
        status = TaskState.Success,
        result = Some(result),
        cacheKey = cacheKey)
      var newRun = replace(run, NodeStatus, newNodeStatus)
      newRun = scheduleNextNodes(newRun, nodeName)
      updateProgress(newRun, parent)
    }
  }

  private def replace(run: Run, nodeStatus: thrift.NodeStatus, newNodeStatus: thrift.NodeStatus) = {
    run.copy(state = run.state.copy(nodes = run.state.nodes - nodeStatus + newNodeStatus))
  }

  private def schedule(run: Run, nodes: Set[Node]): Run = {
    var newRun = run
    nodes.foreach(node => newRun = schedule(newRun, node))
    newRun
  }

  private def schedule(run: Run, node: Node): Run = {
    var newRun = schedulerService.submit(run, node)
    val newNodeStatus = newRun.state.nodes.find(_.name == node.name).get
    if (newNodeStatus.result.isDefined) {
      newRun = onStart(newRun, node.name)
      newRun = scheduleNextNodes(newRun, node.name)
    }
    newRun
  }

  private def updateProgress(run: Run, parent: Option[Run]): (Run, Option[Run]) = {
    val now = System.currentTimeMillis()
    var newRun = run
    if (newRun.state.completedAt.isEmpty) {
      if (newRun.state.nodes.forall(s => Utils.isCompleted(s.status))) {
        // Mark run as completed, if all nodes are completed. It is successful if all nodes were successful.
        val newTaskState = if (newRun.state.nodes.forall(_.status == TaskState.Success)) {
          TaskState.Success
        } else if (newRun.state.nodes.exists(_.status == TaskState.Killed)) {
          TaskState.Killed
        } else {
          TaskState.Failed
        }
        newRun = newRun.copy(state = newRun.state.copy(progress = 1, status = newTaskState, completedAt = Some(now)))
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
        val siblings = storage.read(_.runs.list(RunQuery(parent = Some(parent.id)))).results.filter(_.id != run.id) ++ Seq(run)
        if (siblings.forall(s => Utils.isCompleted(s.state.status))) {
          // Mark parent run as completed if all children are completed. It is successful if all runs were successful.
          val newRunStatus = if (siblings.forall(_.state.status == TaskState.Success)) {
            TaskState.Success
          } else if (siblings.exists(_.state.status == TaskState.Killed)) {
            TaskState.Killed
          } else {
            TaskState.Failed
          }
          parent.copy(state = parent.state.copy(progress = 1, status = newRunStatus, completedAt = Some(System.currentTimeMillis())))
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
        run.state.nodes.find(_.name == dep).get.status == TaskState.Success
      }
    }
    schedule(run, nextNodes)
  }

  private def cancelNextNodes(run: Run, nodeName: String): Run = {
    val graph = graphs(run.pkg)
    val nextNodes = getNextNodes(run, graph, nodeName).flatMap(node => Seq(node) ++ getNextNodes(run, graph, node.name))
    var newRun = run
    nextNodes.foreach { nextNode =>
      val NodeStatus = newRun.state.nodes.find(_.name == nextNode.name).get
      val newNodeStatus = NodeStatus.copy(completedAt = Some(System.currentTimeMillis()), status = TaskState.Cancelled)
      newRun = replace(newRun, NodeStatus, newNodeStatus)
    }
    newRun
  }

  private def cancelAllNodes(run: Run): Run = {
    val graph = graphs(run.pkg)
    var newRun = run
    graph.nodes.foreach { node =>
      val NodeStatus = newRun.state.nodes.find(_.name == node.name).get
      if (!Utils.isCompleted(NodeStatus.status)) {
        val newNodeStatus = NodeStatus.copy(completedAt = Some(System.currentTimeMillis()), status = TaskState.Cancelled)
        newRun = replace(newRun, NodeStatus, newNodeStatus)
      }
    }
    newRun
  }

  private def getNextNodes(run: Run, graph: Graph, nodeName: String): Set[Node] = {
    graph(nodeName).successors
      .map(graph.apply)
      .filter { nextNode =>
        run.state.nodes.find(_.name == nextNode.name).get.status == TaskState.Waiting
      }
  }
}
