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

import java.nio.file.{Files, Path, StandardOpenOption}

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.storage.{LogsQuery, MutableRunRepository, RunList, RunQuery}
import fr.cnrs.liris.common.util.FileUtils

import scala.collection.JavaConverters._

/**
 * A run repository storing runs locally inside binary files. Intended for testing or use in single-node development
 * clusters. It might have very poor performance because data is not indexed, which results in a sequential scan at
 * each query. Moreover, this implementation does no result memoization.
 *
 * @param config Local repository configuration.
 */
@Singleton
final class LocalRunRepository @Inject()(config: LocalStorageConfig) extends LocalRepository with MutableRunRepository {
  Files.createDirectories(runPath)
  Files.createDirectories(logPath)

  override def find(query: RunQuery): RunList = {
    var results = listIds(runPath)
      .flatMap(id => get(RunId(id)))
      .filter(query.matches)
      .sortWith((a, b) => a.createdAt > b.createdAt)

    val totalCount = results.size
    query.offset.foreach { offset => results = results.drop(offset) }
    query.limit.foreach { limit => results = results.take(limit) }

    // Remove the result of each node, that we do not want to return.
    results = results.map(run => run.copy(state = run.state.copy(nodes = run.state.nodes.map(_.unsetResult))))

    RunList(results, totalCount)
  }

  override def find(query: LogsQuery): Seq[RunLog] = {
    val files = query.classifier match {
      case Some(classifier) => Seq(logPath(query.runId, query.nodeName, classifier).toFile)
      case None => logPath(query.runId, query.nodeName).toFile.listFiles.toSeq.filter(_.getName.endsWith(".txt"))
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
  }

  override def save(logs: Seq[RunLog]): Unit = {
    logs.groupBy(_.runId).foreach { case (runId, logs) =>
      logs.groupBy(_.nodeName).foreach { case (nodeName, logs) =>
        logs.groupBy(_.classifier).foreach { case (classifier, logs) =>
          val path = logPath(runId, nodeName, classifier)
          Files.createDirectories(path.getParent)
          val content = logs.map(log => s"${log.createdAt} ${log.message.replace("\n", "\\\\n")}\n").mkString
          //TODO: lock file? Or not ?
          Files.write(path, content.getBytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        }
      }
    }
  }

  override def get(id: RunId): Option[Run] = read(runPath(id), Run)

  override def remove(id: RunId): Unit = {
    FileUtils.safeDelete(runPath(id))
    FileUtils.safeDelete(logPath(id))
  }

  override def get(cacheKey: CacheKey): Option[OpResult] = None

  private def runPath: Path = config.path.resolve("runs")

  private def logPath: Path = config.path.resolve("logs")

  private def runPath(id: RunId): Path = getSubdir(runPath, id.value).resolve(s"${id.value}.json")

  private def logPath(id: RunId): Path = getSubdir(logPath, id.value.toString).resolve(id.value.toString)

  private def logPath(id: RunId, nodeName: String): Path = logPath(id).resolve(nodeName)

  private def logPath(id: RunId, nodeName: String, classifier: String): Path = logPath(id, nodeName).resolve(s"$classifier.txt")

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