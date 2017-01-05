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

package fr.cnrs.liris.accio.core.framework.local

import java.nio.file.Path

import com.twitter.finatra.json.FinatraObjectMapper
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.common.util.FileUtils

/**
 * An run repository storing workflows locally inside JSON files. Intended for testing or use in single-node
 * development clusters. It might have very poor performance because data is not indexed, which results in a sequential
 * scan for each query.
 *
 * @param rootPath Root directory where to store artifacts.
 * @param mapper   Object mapper.
 */
class LocalWorkflowRepository(rootPath: Path, mapper: FinatraObjectMapper)
  extends LocalRepository[Workflow](mapper, locking = false) with WorkflowRepository {

  override def find(query: WorkflowQuery): WorkflowList = {
    var results = rootPath.toFile.listFiles.toSeq
      .filter(_.isDirectory)
      .map(_.getName)
      .flatMap(dirname => get(WorkflowId(dirname)))

    // 1. Filter results by specified criteria.
    query.owner.foreach { owner => results = results.filter(_.owner == owner) }
    query.name.foreach { name => results = results.filter(_.name.contains(name)) }

    // 2. Sort the results in descending chronological order (after filtering and before slicing).
    results = results.sortWith((a, b) => a.createdAt.getMillis < b.createdAt.getMillis)

    // 3. Count total number of results (before slicing).
    val totalCount = results.size

    // 4. Slice results w.r.t. to specified offset and limit.
    query.offset.foreach { offset => results = results.drop(offset) }
    query.limit.foreach { limit => results = results.take(limit) }

    WorkflowList(results, totalCount)
  }

  override def save(workflow: Workflow): Workflow = {
    val version = if (workflow.version == 0) {
      getLastVersion(workflow.id).getOrElse(0) + 1
    } else {
      if (exists(workflow.id, workflow.version)) {
        throw new IllegalArgumentException(s"Workflow ${workflow.id.value}:${workflow.version} already exists")
      }
      workflow.version
    }
    write(workflow, getPath(workflow.id, version).toFile)
    workflow.copy(version = version)
  }

  override def get(id: WorkflowId): Option[Workflow] = getLastVersion(id).flatMap(get(id, _))

  override def get(id: WorkflowId, version: Int): Option[Workflow] = {
    read(getPath(id, version).toFile)
  }

  override def exists(id: WorkflowId): Boolean = getLastVersion(id).isDefined

  override def exists(id: WorkflowId, version: Int): Boolean = {
    getPath(id, version).toFile.exists
  }

  override def delete(id: WorkflowId): Unit = {
    FileUtils.safeDelete(getPath(id))
  }

  private def getPath(id: WorkflowId) = rootPath.resolve(id.value)

  private def getPath(id: WorkflowId, version: Int) = rootPath.resolve(id.value).resolve(s"$version.json")

  private def getLastVersion(id: WorkflowId): Option[Int] = {
    val dir = getPath(id).toFile
    if (dir.exists()) {
      dir.list.filter(_.endsWith(".json")).map(_.toInt).sorted.lastOption
    } else {
      None
    }
  }
}