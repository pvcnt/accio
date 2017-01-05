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
 * An run repository storing runs locally inside JSON files. Intended for testing or use in single-node development
 * clusters. It might have very poor performance because data is not indexed, which results in a sequential scan for
 * each query.
 *
 * @param rootPath Root directory where to store artifacts.
 * @param mapper   Object mapper.
 */
class LocalRunRepository(rootPath: Path, mapper: FinatraObjectMapper)
  extends LocalRepository[Run](mapper, locking = false) with RunRepository {

  override def find(query: RunQuery): RunList = {
    var results = rootPath.toFile.listFiles.toSeq
      .flatMap(_.listFiles)
      .flatMap(_.list)
      .filter(_.endsWith(".json"))
      .flatMap(filename => get(RunId(filename.stripSuffix(".json"))))

    // 1. Filter results by specified criteria.
    query.workflow.foreach { workflow => results = results.filter(_.pkg.workflowId == workflow) }
    query.name.foreach { name => results = results.filter(_.name.contains(name)) }
    query.cluster.foreach { cluster => results = results.filter(_.cluster == cluster) }
    query.owner.foreach { owner => results = results.filter(_.owner == owner) }
    query.environment.foreach { environment => results = results.filter(_.environment == environment) }

    // 2. Sort the results in descending chronological order (after filtering and before slicing).
    results = results.sortWith((a, b) => a.createdAt.getMillis < b.createdAt.getMillis)

    // 3. Count total number of results (before slicing).
    val totalCount = results.size

    // 4. Slice results w.r.t. to specified offset and limit.
    query.offset.foreach { offset => results = results.drop(offset) }
    query.limit.foreach { limit => results = results.take(limit) }

    RunList(results, totalCount)
  }

  override def save(run: Run): Unit = {
    write(run, getPath(run.id).toFile)
  }

  override def delete(id: RunId): Unit = {
    FileUtils.safeDelete(getPath(id))
  }

  override def get(id: RunId): Option[Run] = read(getPath(id).toFile)

  override def exists(id: RunId): Boolean = getPath(id).toFile.exists()

  private def getPath(id: RunId) = {
    // We create two levels of subdirectories based on run identifier to avoid putting to many files in a single
    // directory. Filesystems are not usually very good at dealing with this.
    rootPath
      .resolve(id.value.substring(0, 1))
      .resolve(id.value.substring(1, 2))
      .resolve(s"${id.value}.json")
  }
}
