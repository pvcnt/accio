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

package fr.cnrs.liris.accio.core.storage.local

import java.nio.file.{Files, Path}

import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.storage.{MutableWorkflowRepository, WorkflowList, WorkflowQuery}
import fr.cnrs.liris.common.util.FileUtils

/**
 * A workflow repository storing workflows locally inside binary files. Intended for testing or use in single-node
 * development clusters. It might have very poor performance because data is not indexed, which results in a
 * sequential scan at each query.
 *
 * @param config Local repository configuration.
 */
@Singleton
final class LocalWorkflowRepository @Inject()(config: LocalStorageConfig)
  extends LocalRepository with MutableWorkflowRepository with LazyLogging {

  Files.createDirectories(workflowPath)

  override def find(query: WorkflowQuery): WorkflowList = {
    var results = workflowPath.toFile.listFiles.toSeq
      .filter(_.isDirectory)
      .map(_.getName)
      .flatMap(dirname => get(WorkflowId(dirname)))
      .filter(query.matches)
      .sortWith((a, b) => a.createdAt > b.createdAt)

    val totalCount = results.size
    query.offset.foreach { offset => results = results.drop(offset) }
    results = results.take(query.limit)

    // Remove the graph of each workflow, that we do not want to return.
    results = results.map(workflow => workflow.copy(graph = workflow.graph.unsetNodes))

    WorkflowList(results, totalCount)
  }

  override def save(workflow: Workflow): Unit = {
    if (contains(workflow.id, workflow.version)) {
      logger.info(s"Workflow ${workflow.id} @ ${workflow.version} already exists")
    } else {
      write(workflow, workflowPath(workflow.id, workflow.version).toFile)
      FileUtils.safeDelete(latestPath(workflow.id))
      Files.createSymbolicLink(latestPath(workflow.id), workflowPath(workflow.id, workflow.version))
    }
  }

  override def get(id: WorkflowId): Option[Workflow] = read(latestPath(id), Workflow)

  override def get(id: WorkflowId, version: String): Option[Workflow] = read(id, version)

  private def contains(id: WorkflowId, version: String): Boolean = workflowPath(id, version).toFile.exists

  private def workflowPath: Path = config.path.resolve("workflows")

  private def workflowPath(id: WorkflowId): Path = workflowPath.resolve(id.value)

  private def workflowPath(id: WorkflowId, version: String): Path = workflowPath(id).resolve(s"v$version.json")

  private def latestPath(id: WorkflowId): Path = workflowPath(id).resolve(s"latest.json")

  private def read(id: WorkflowId, version: String): Option[Workflow] = {
    read(workflowPath(id, version), Workflow).map { workflow =>
      val latestVersion = Files.readSymbolicLink(latestPath(id)).getFileName.toString.drop(1).stripSuffix(".json")
      if (latestVersion != version) {
        workflow.copy(isActive = false)
      } else {
        workflow
      }
    }
  }
}