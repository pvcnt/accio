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

package fr.cnrs.liris.accio.discovery

import java.io.{BufferedInputStream, FileInputStream}
import java.nio.file.{Files, Path}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ConcurrentHashMap, Executors}

import com.twitter.util.Duration
import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.domain.thrift.ThriftAdapter
import fr.cnrs.liris.accio.domain.{Operator, thrift}
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.control.NonFatal

final class FileOpDiscovery(directory: Path, checkFrequency: Duration, filter: Option[String])
  extends OpDiscovery with Logging {

  private[this] val libraries = new ConcurrentHashMap[String, Library].asScala
  private[this] val executor = Executors.newSingleThreadExecutor()
  executor.submit(CheckThread)

  override def ops: Iterable[Operator] = libraries.values.flatMap(_.ops)

  private def updateLibraries(): Unit = {
    var it = Files.list(directory).iterator.asScala.filter(Files.isRegularFile(_))
    filter.foreach { filter =>
      val regex = filter.r
      it = it.filter(p => regex.findFirstIn(p.getFileName.toString).nonEmpty)
    }
    val keys = it.map { file =>
      val lastModified = Files.getLastModifiedTime(file).toMillis
      val key = file.toAbsolutePath.toString
      if (!libraries.contains(key) || libraries(key).lastModified < lastModified) {
        readLibrary(file, lastModified).foreach { library =>
          libraries.update(key, library)
          logger.info(s"Updated library defined at $key")
        }
      }
      key
    }.toSet
    libraries.keySet.diff(keys).foreach { key =>
      libraries.remove(key)
      logger.info(s"Removed library defined at $key")
    }
  }

  private def readLibrary(file: Path, lastModified: Long): Option[Library] = {
    val fis = new BufferedInputStream(new FileInputStream(file.toFile))
    try {
      val ops = mutable.Set.empty[Operator]
      while (fis.available() > 0) {
        ops += ThriftAdapter.toDomain(BinaryScroogeSerializer.read(fis, thrift.Operator))
      }
      Some(Library(ops.toSet, lastModified))
    } catch {
      case NonFatal(e) =>
        logger.warn(s"Error while reading library defined at $file", e)
        None
    } finally {
      fis.close()
    }
  }

  private case class Library(ops: Set[Operator], lastModified: Long)

  private object CheckThread extends Runnable {
    private[this] val killed = new AtomicBoolean(false)

    override def run(): Unit = {
      logger.info(s"Starting thread to check updates every $checkFrequency")
      while (!killed.get) {
        try {
          updateLibraries()
          Thread.sleep(checkFrequency.inMillis)
        } catch {
          case _: InterruptedException =>
            // If the thread as been interrupted, we kill it properly.
            Thread.currentThread().interrupt()
            killed.set(true)
          case NonFatal(e) =>
            // Catch and suppress any other error in order to avoid this thread to die prematurely.
            logger.error("Error while updating libraries", e)
        }
      }
    }
  }

}
