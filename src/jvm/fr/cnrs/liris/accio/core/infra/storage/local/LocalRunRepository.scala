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

package fr.cnrs.liris.accio.core.infra.storage.local

import java.nio.file.{Files, Path, StandardOpenOption}

import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.infra.util.LocalStorage
import fr.cnrs.liris.common.util.FileUtils

import scala.collection.JavaConverters._

/**
 * A run repository storing runs locally inside binary files. Intended for testing or use in single-node development
 * clusters. It might have very poor performance because data is not indexed, which results in a sequential scan at
 * each query. Moreover, this implementation does no result memoization.
 *
 * @param rootDir Root directory under which to store files.
 */
final class LocalRunRepository(rootDir: Path) extends LocalStorage with MutableRunRepository with StrictLogging {
  override def find(query: RunQuery): RunList = {
    var results = listIds(runsPath).flatMap(id => get(RunId(id)))

    // Filter results by specified criteria.
    query.workflow.foreach { workflow => results = results.filter(_.pkg.workflowId == workflow) }
    query.name.foreach { name => results = results.filter(_.name.contains(name)) }
    query.owner.foreach { owner => results = results.filter(_.owner.name == owner) }
    if (query.status.nonEmpty) {
      results = results.filter(run => query.status.contains(run.state.status))
    }
    query.parent match {
      case Some(parent) => results = results.filter(_.parent.contains(parent))
      case None => results = results.filter(_.parent.isEmpty)
    }
    query.clonedFrom.foreach { clonedFrom => results = results.filter(_.clonedFrom.contains(clonedFrom)) }

    // Sort results in descending chronological order, count and slice.
    results = results.sortWith((a, b) => a.createdAt > b.createdAt)
    val totalCount = results.size
    query.offset.foreach { offset => results = results.drop(offset) }
    query.limit.foreach { limit => results = results.take(limit) }

    // Remove the result of each node, that we do not want to return.
    results = results.map(run => run.copy(state = run.state.copy(nodes = run.state.nodes.map(_.unsetResult))))

    RunList(results, totalCount)
  }

  override def find(query: LogsQuery): Seq[RunLog] = {
    val files = query.classifier match {
      case Some(classifier) => Seq(logsPath(query.runId, query.nodeName, classifier).toFile)
      case None => logsPath(query.runId, query.nodeName).toFile.listFiles.toSeq.filter(_.getName.endsWith(".txt"))
    }
    var results = files.flatMap { file =>
      val classifier = file.getName.stripSuffix(".txt")
      val lines = Files.readAllLines(file.toPath).asScala
      lines.flatMap { line =>
        val pos = line.indexOf(" ")
        val log = RunLog(query.runId, query.nodeName, line.take(pos).toLong, classifier, line.drop(pos + 1))
        if (query.since.isEmpty || log.createdAt > query.since.get.inMillis) Some(log) else None
      }
    }.sortWith((a, b) => a.createdAt < b.createdAt)
    query.limit.foreach { limit =>
      results = results.take(limit)
    }
    results
  }

  override def save(run: Run): Unit = {
    write(run, runPath(run.id))
    logger.debug(s"Saved run ${run.id.value}")
  }

  override def save(logs: Seq[RunLog]): Unit = {
    logs.groupBy(_.runId).foreach { case (runId, logs) =>
      logs.groupBy(_.nodeName).foreach { case (nodeName, logs) =>
        logs.groupBy(_.classifier).foreach { case (classifier, logs) =>
          val path = logsPath(runId, nodeName, classifier)
          Files.createDirectories(path.getParent)
          val content = logs.map(log => s"${log.createdAt} ${log.message.replace("\n", "\\\\n")}\n").mkString
          //TODO: lock file? Or not ?
          Files.write(path, content.getBytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        }
      }
    }
    logger.debug(s"Saved ${logs.size} logs")
  }

  override def get(id: RunId): Option[Run] = read(runPath(id), Run)

  override def contains(id: RunId): Boolean = runPath(id).toFile.exists()

  override def remove(id: RunId): Unit = {
    FileUtils.safeDelete(runPath(id))
    FileUtils.safeDelete(logsPath(id))
    logger.debug(s"Removed run ${id.value}")
  }

  override def get(cacheKey: CacheKey): Option[OpResult] = None

  private def runsPath = rootDir.resolve("runs")

  private def logsPath = rootDir.resolve("logs")

  private def runPath(id: RunId) = getSubdir(runsPath, id.value).resolve(s"${id.value}.json")

  private def logsPath(id: RunId): Path = getSubdir(logsPath, id.value.toString).resolve(id.value.toString)

  private def logsPath(id: RunId, nodeName: String): Path = logsPath(id).resolve(nodeName)

  private def logsPath(id: RunId, nodeName: String, classifier: String): Path = logsPath(id, nodeName).resolve(s"$classifier.txt")

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