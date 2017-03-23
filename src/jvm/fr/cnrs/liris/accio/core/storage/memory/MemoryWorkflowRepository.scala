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

package fr.cnrs.liris.accio.core.storage.memory

import java.util.concurrent.ConcurrentHashMap

import com.google.common.annotations.VisibleForTesting
import com.google.common.util.concurrent.AbstractIdleService
import com.google.inject.Singleton
import fr.cnrs.liris.accio.core.domain.{Workflow, WorkflowId}
import fr.cnrs.liris.accio.core.storage.{MutableWorkflowRepository, WorkflowList, WorkflowQuery}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Run repository storing data in memory. It has no persistence mechanism. Intended for testing only.
 */
@Singleton
@VisibleForTesting
final class MemoryWorkflowRepository extends AbstractIdleService with MutableWorkflowRepository {
  private[this] val index = new ConcurrentHashMap[WorkflowId, mutable.Map[String, Workflow]]().asScala

  override def save(workflow: Workflow): Unit = {
    val workflows = index.getOrElseUpdate(workflow.id, new ConcurrentHashMap[String, Workflow]().asScala)
    if (workflow.isActive) {
      workflows.foreach { case (version, oldWorkflow) =>
        workflows(version) = oldWorkflow.copy(isActive = false)
      }
    }
    workflows(workflow.version) = workflow
  }

  override def find(query: WorkflowQuery): WorkflowList = {
    var results = index.values
      .flatMap(_.values.find(_.isActive))
      .filter(query.matches)
      .toSeq
      .sortWith((a, b) => a.createdAt > b.createdAt)

    val totalCount = results.size
    query.offset.foreach { offset => results = results.drop(offset) }
    results = results.take(query.limit)

    // Remove the graph of each workflow, that we do not want to return.
    results = results.map(workflow => workflow.copy(graph = workflow.graph.unsetNodes))

    WorkflowList(results, totalCount)
  }

  override def get(id: WorkflowId): Option[Workflow] = {
    index.get(id).flatMap(_.values.find(_.isActive))
  }

  override def get(id: WorkflowId, version: String): Option[Workflow] = {
    index.get(id).flatMap(_.get(version))
  }

  override protected def shutDown(): Unit = {}

  override protected def startUp(): Unit = {}
}
