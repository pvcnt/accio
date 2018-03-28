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

package fr.cnrs.liris.accio.storage.memory

import java.util.concurrent.ConcurrentHashMap

import com.twitter.finagle.stats.StatsReceiver
import fr.cnrs.liris.accio.api.ResultList
import fr.cnrs.liris.accio.api.thrift.{Workflow, WorkflowId}
import fr.cnrs.liris.accio.storage.{WorkflowQuery, WorkflowStore}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Run repository storing data in memory. Intended for testing only.
 *
 * @param statsReceiver Stats receiver.
 */
private[memory] final class MemoryWorkflowRepository(statsReceiver: StatsReceiver)
  extends WorkflowStore.Mutable {

  private[this] val index = new ConcurrentHashMap[WorkflowId, mutable.Map[String, Workflow]]().asScala
  statsReceiver.provideGauge("storage", "memory", "workflow", "size")(index.values.map(_.size).sum)

  override def list(query: WorkflowQuery): ResultList[Workflow] = {
    var results = index.values
      .flatMap(_.values.find(_.isActive))
      .filter(query.matches)
      .toSeq
      .sortWith((a, b) => a.createdAt.get > b.createdAt.get)

    val totalCount = results.size
    query.offset.foreach { offset => results = results.drop(offset) }
    query.limit.foreach { limit => results = results.take(limit) }

    // Remove the graph of each workflow, that we do not want to return.
    results = results.map(workflow => workflow.copy(graph = workflow.graph.unsetNodes))

    ResultList(results, totalCount)
  }

  override def get(id: WorkflowId): Option[Workflow] = {
    index.get(id).flatMap(_.values.find(_.isActive))
  }

  override def get(id: WorkflowId, version: String): Option[Workflow] = {
    index.get(id).flatMap(_.get(version))
  }

  override def save(workflow: Workflow): Unit = {
    val workflows = index.getOrElseUpdate(workflow.id, new ConcurrentHashMap[String, Workflow]().asScala)
    if (workflow.isActive) {
      workflows.foreach { case (version, oldWorkflow) =>
        workflows(version) = oldWorkflow.copy(isActive = false)
      }
    }
    workflows(workflow.version.get) = workflow
  }
}
