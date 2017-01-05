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

package fr.cnrs.liris.accio.core.runtime

import java.nio.file.{Files, Path}

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.framework._

import scala.collection.mutable
import scala.util.control.NonFatal

/**
 *
 * @param runRepository      Run repository.
 * @param workflowRepository Workflow repository.
 * @param nodeExecutor       Node executor.
 * @param workDir            Accio working directory.
 */
class RunController @Inject()(
  runRepository: RunRepository,
  workflowRepository: WorkflowRepository,
  nodeExecutor: NodeExecutor,
  scheduler: Scheduler,
  workDir: Path)
  extends LazyLogging {

  /**
   * Execute a single run.
   *
   * @param run      Run to execute.
   * @param reporter Reporter receiving progress updates.
   */
  def execute(run: Run, reporter: ProgressReporter): Unit = {
    var r = run.updated(_.started)
    writeReport(r, reporter.onStart(r))

    val workflow = workflowRepository.get(run.pkg.workflowId, run.pkg.workflowVersion).get

    // Initially schedule graph roots
    val scheduled = mutable.Queue.empty[Node] ++ workflow.graph.roots
    var runSuccessful = true

    while (scheduled.nonEmpty) {
      val node = scheduled.dequeue()
      r = r.updated(_.nodeStarted(node.name))
      writeReport(r, reporter.onStart(r, node))

      val (nodeKey, successful) = nodeExecutor.execute(r, node, workDir.resolve(s"sandbox/${run.id}/${node.name}"))
      val progress = (r.status.completedNodes.size.toDouble + 1) / workflow.graph.size
      r.updated(_.nodeCompleted(node.name, nodeKey, progress))
      writeReport(r, reporter.onComplete(r, node))

      runSuccessful &= successful

      // Then we can schedule next steps to take.
      if (successful) {
        // If the node was successfully executed, we schedule its successors for which all dependencies are available.
        scheduled ++= node.successors.map(workflow.graph(_)).filter(_.predecessors.forall(r.status.completedNodes.contains))
      } else {
        // Otherwise we clear the remaining scheduled nodes to exit the loop.
        scheduled.clear()
      }
    }

    r = r.updated(_.completed(runSuccessful))
    writeReport(r, reporter.onComplete(r))

    // Last verification that all nodes had a chance to run. It is more an assertion that should never raise an error.
    if (runSuccessful && r.status.completedNodes.size != workflow.graph.size) {
      val missing = workflow.graph.nodes.map(_.name).diff(r.status.completedNodes.keySet)
      throw new IllegalStateException(s"Some nodes were never executed: ${missing.mkString(", ")}")
    }
  }

  private def writeReport(run: Run, f: => Unit) = {
    try {
      runRepository.save(run)
    } catch {
      case NonFatal(e) => logger.error(s"Unable to write report for run ${run.id}", e)
    }
    try {
      f
    } catch {
      case NonFatal(e) => logger.error("Error while reporting progress", e)
    }
  }
}