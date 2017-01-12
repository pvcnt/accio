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

package fr.cnrs.liris.accio.core.infrastructure.storage.local

import java.nio.file.{Files, Path}

import com.google.inject.Inject
import com.twitter.finatra.json.FinatraObjectMapper
import fr.cnrs.liris.accio.core.application.Configurable
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.common.util.FileUtils

/**
 * Local run repository configuration.
 *
 * @param rootDir Root directory where to store files.
 */
case class LocalRunRepositoryConfig(rootDir: Path)

/**
 * A run repository storing runs locally inside JSON files.
 *
 * Intended for testing or use in single-node development clusters. It might have very poor performance because data
 * is not indexed, which results in a sequential scan at each query.
 *
 * @param mapper Object mapper.
 */
final class LocalRunRepository @Inject()(mapper: FinatraObjectMapper)
  extends LocalRepository(mapper, locking = false) with RunRepository with Configurable[LocalRunRepositoryConfig] {

  override def configClass: Class[LocalRunRepositoryConfig] = classOf[LocalRunRepositoryConfig]

  override def find(query: RunQuery): RunList = {
    var results = listIds(runsPath).flatMap(id => get(RunId(id)))

    // 1. Filter results by specified criteria.
    query.workflow.foreach { workflow => results = results.filter(_.pkg.workflowId == workflow) }
    query.name.foreach { name => results = results.filter(_.name.contains(name)) }
    query.cluster.foreach { cluster => results = results.filter(_.entitlement.cluster == cluster) }
    query.owner.foreach { owner => results = results.filter(_.entitlement.user.name == owner) }
    query.environment.foreach { environment => results = results.filter(_.entitlement.environment == environment) }
    query.status.foreach { status => results = results.filter(_.state.status == status) }

    // 2. Sort the results in descending chronological order.
    results = results.sortWith((a, b) => a.createdAt < b.createdAt)

    // 3. Count total number of results (before slicing).
    val totalCount = results.size

    // 4. Slice results w.r.t. to specified offset and limit.
    query.offset.foreach { offset => results = results.drop(offset) }
    query.limit.foreach { limit => results = results.take(limit) }

    RunList(results, totalCount)
  }

  override def find(query: TaskQuery): TaskList = {
    var results = listIds(tasksPath).flatMap(id => get(TaskId(id)))

    // 1. Filter results by specified criteria.
    query.runId.foreach { runId => results = results.filter(_.runId == runId) }
    query.status.foreach { status => results = results.filter(_.state.status == status) }

    // 2. Sort the results in descending chronological order.
    results = results.sortWith((a, b) => a.createdAt < b.createdAt)

    // 3. Count total number of results (before slicing).
    val totalCount = results.size

    // 4. Slice results w.r.t. to specified offset and limit.
    query.offset.foreach { offset => results = results.drop(offset) }
    query.limit.foreach { limit => results = results.take(limit) }

    TaskList(results, totalCount)
  }

  override def find(query: LogsQuery): Seq[TaskLog] = {
    //TODO
    var results = Seq.empty[TaskLog]

    // 1. Filter results by specified criteria.
    query.classifier.foreach { classifier => results = results.filter(_.classifier == classifier) }
    query.since.foreach { since => results = results.filter(_.createdAt > since.inMillis) }

    // 2. Sort the results in descending chronological order and slice them.
    results = results.sortWith((a, b) => a.createdAt < b.createdAt)
    query.limit.foreach { limit => results = results.take(limit) }

    results
  }

  override def save(run: Run): Unit = {
    write(run, getPath(run.id).toFile)
  }

  override def save(task: Task): Unit = {
    write(task, getPath(task.id).toFile)
  }

  override def save(logs: Seq[TaskLog]): Unit = {
    logs.groupBy(_.taskId)
      .foreach { case (taskId, logs) =>
        logs.groupBy(_.classifier).foreach { case (classifier, logs) =>
          val content = logs.map(log => s"${log.createdAt} ${log.message.replace("\n", "\\\\n")}").mkString("\n")
          Files.write(getSubdir(logsPath, taskId.value.toString).resolve(s"$classifier.txt"), content.getBytes)
        }
      }
  }

  override def get(id: RunId): Option[Run] = read[Run](getPath(id).toFile)

  override def get(id: TaskId): Option[Task] = read[Task](getPath(id).toFile)

  override def exists(id: RunId): Boolean = getPath(id).toFile.exists()

  override def exists(id: TaskId): Boolean = getPath(id).toFile.exists()

  override def delete(id: RunId): Unit = {
    FileUtils.safeDelete(getPath(id))
  }

  private def runsPath = config.rootDir.resolve("runs")

  private def tasksPath = config.rootDir.resolve("tasks")

  private def logsPath = config.rootDir.resolve("logs")

  private def getPath(id: RunId) = {
    getSubdir(runsPath, id.value).resolve(s"${id.value}.json")
  }

  private def getPath(id: TaskId) = {
    getSubdir(tasksPath, id.value).resolve(s"${id.value}.json")
  }

  private def getSubdir(dir: Path, id: String) = {
    // We create two levels of subdirectories based on run identifier to avoid putting to many files in a single
    // directory. Filesystems are not usually very good at dealing with this.
    dir
      .resolve(id.substring(0, 1))
      .resolve(id.substring(1, 2))
  }

  private def listIds(dir: Path) = {
    dir.toFile
      .listFiles.toSeq
      .flatMap(_.listFiles)
      .flatMap(_.list)
      .filter(_.endsWith(".json"))
      .map(_.stripSuffix(".json"))
  }
}