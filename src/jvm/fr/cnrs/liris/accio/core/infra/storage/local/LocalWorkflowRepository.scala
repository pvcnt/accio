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

package fr.cnrs.liris.accio.core.infra.storage.local

import java.nio.file.Path

import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.infra.util.LocalStorage

/**
 * A workflow repository storing workflows locally inside binary files. Intended for testing or use in single-node
 * development clusters. It might have very poor performance because data is not indexed, which results in a
 * sequential scan at each query.
 *
 * @param rootDir Root directory under which to store files.
 */
final class LocalWorkflowRepository(rootDir: Path)
  extends LocalStorage(locking = false) with WorkflowRepository with LazyLogging {

  override def find(query: WorkflowQuery): WorkflowList = {
    var results = rootDir.toFile.listFiles.toSeq
      .filter(_.isDirectory)
      .map(_.getName)
      .flatMap(dirname => get(WorkflowId(dirname)))

    // 1. Filter results by specified criteria.
    query.owner.foreach { owner => results = results.filter(_.owner == owner) }
    query.name.foreach { name => results = results.filter(_.name.contains(name)) }

    // 2. Sort the results in descending chronological order (after filtering and before slicing).
    results = results.sortWith((a, b) => a.createdAt < b.createdAt)

    // 3. Count total number of results (before slicing).
    val totalCount = results.size

    // 4. Slice results w.r.t. to specified offset and limit.
    query.offset.foreach { offset => results = results.drop(offset) }
    query.limit.foreach { limit => results = results.take(limit) }

    WorkflowList(results, totalCount)
  }

  override def save(workflow: Workflow): Unit = {
    if (exists(workflow.id, workflow.version)) {
      logger.info(s"Workflow ${workflow.id} @ ${workflow.version} already exists")
    } else {
      write(workflow, workflowPath(workflow.id, workflow.version).toFile)
    }
  }

  override def get(id: WorkflowId): Option[Workflow] = getLastVersion(id).flatMap(v => get(id, v.toString))

  override def get(id: WorkflowId, version: String): Option[Workflow] = {
    read(workflowPath(id, version).toFile, Workflow)
  }

  override def exists(id: WorkflowId): Boolean = getLastVersion(id).isDefined

  override def exists(id: WorkflowId, version: String): Boolean = {
    workflowPath(id, version).toFile.exists
  }

  private def workflowPath(id: WorkflowId) = rootDir.resolve(id.value)

  private def workflowPath(id: WorkflowId, version: String) = rootDir.resolve(id.value).resolve(s"$version.json")

  private def getLastVersion(id: WorkflowId): Option[String] = {
    //TODO
    val dir = workflowPath(id).toFile
    if (dir.exists()) {
      dir.list.filter(_.endsWith(".json")).map(_.toInt).sorted.lastOption.map(_.toString)
    } else {
      None
    }
  }
}