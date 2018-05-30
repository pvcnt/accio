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

import java.io._
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ConcurrentHashMap, Executors}

import com.google.common.io.ByteStreams
import com.twitter.util._
import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.domain.thrift.ThriftAdapter
import fr.cnrs.liris.accio.domain.{Operator, thrift}
import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.util.jvm.JavaHome
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.control.NonFatal

/**
 * Discover operator libraries by scanning a directory. The scan can be done only once at startup,
 * or scheduled automatically at regular intervals. If doing so, we recommend setting a high
 * enough frequency (e.g., at least tens of seconds), as this operation involves file I/O and
 * possibly launching sub-processes.
 *
 * This directory can contain either binary library files (e.g., JAR files) or descriptor files
 * whose content is a list of operators, encoded as their associated Thrift structure
 * ([[thrift.Operator]]). The former are executed and the list of operators is extracted from their
 * standard output while the content of the latter is directly read and interpreted as a list of
 * operators. The distinction between these two kinds of files is hard-coded and depends on the
 * file extensions.
 *
 * @param directory      Directory in which to look for operator libraries. Only the files directly
 *                       under that directory will be considered (i.e., sub-directories are ignored).
 * @param checkFrequency Frequency at which to check for file changes. 0 means disabled.
 * @param filter         Regex used to filter the files. If set, only the files whose name matches
 *                       this regex will be considered.
 */
final class FileOpDiscovery(directory: Path, checkFrequency: Duration, filter: Option[String])
  extends OpDiscovery with Logging {

  private[this] val libraries = new ConcurrentHashMap[String, Library].asScala
  private[this] val closed = new AtomicBoolean(false)
  private[this] val pool = FuturePool(Executors.newSingleThreadExecutor())
  private[this] val binaryExtensions = Set("jar")

  // Read libraries a first time, and then start a loop if automatic updates have been enabled.
  updateLibraries()
  if (checkFrequency > Duration.Zero) {
    logger.info(s"Starting thread to check changes every $checkFrequency")
    pool(BackgroundThread())
  }

  override def ops: Iterable[Operator] = libraries.values.flatMap(_.ops)

  override def close(deadline: Time): Future[Unit] = {
    if (closed.compareAndSet(false, true)) {
      pool.executor.shutdownNow()
    }
    Future.Done
  }

  private def updateLibraries(): Unit = synchronized {
    if (!Files.isDirectory(directory)) {
      logger.warn(s"Directory $directory does not exist")
      libraries.clear()
    } else {
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
            logger.info(s"Updated library defined in ${directory.relativize(file)}")
          }
        }
        key
      }.toSet
      libraries.keySet.diff(keys).foreach { key =>
        libraries.remove(key)
        logger.info(s"Removed library defined in ${directory.relativize(Paths.get(key))}")
      }
    }
  }

  private def readLibrary(file: Path, lastModified: Long): Option[Library] = {
    val maybeOps = if (binaryExtensions.contains(getExtension(file))) {
      readBinary(file)
    } else {
      readDescriptor(file)
    }
    maybeOps.map(ops => Library(ops, lastModified))
  }

  private def readDescriptor(file: Path): Option[Set[Operator]] = {
    val fis = new BufferedInputStream(new FileInputStream(file.toFile))
    decode(fis, file)
  }

  private def readBinary(file: Path): Option[Set[Operator]] = {
    val cmd = mutable.ListBuffer.empty[String]
    if (getExtension(file) == "jar") {
      cmd += JavaHome.javaBinary.toString
      cmd ++= Seq("-Xms100M")
      cmd ++= Seq("-Xmx100M")
      cmd ++= Seq("-jar", file.toAbsolutePath.toString)
    } else {
      cmd += file.toAbsolutePath.toString
    }

    val os = new ByteArrayOutputStream()
    val process = new ProcessBuilder()
      .command(cmd: _*)
      .redirectErrorStream(true)
      .start()
    ByteStreams.copy(process.getInputStream, os)
    val exitCode = process.waitFor()

    if (exitCode != 0) {
      logger.warn(s"Error while executing library defined in ${directory.relativize(file)} " +
        s"(exit code $exitCode):\n${new String(os.toByteArray)}")
      None
    } else {
      val is = new ByteArrayInputStream(os.toByteArray)
      // Binary libraries cannot usually provide their own path, so we replace it with the path
      // to the binary itself.
      val remoteFile =  RemoteFile(file.toAbsolutePath.toString, Some("application/java-archive"))
      decode(is, file).map(_.map(_.copy(executable = remoteFile)))
    }
  }

  private def decode(is: InputStream, file: Path): Option[Set[Operator]] = {
    try {
      val ops = mutable.Set.empty[Operator]
      while (is.available() > 0) {
        ops += ThriftAdapter.toDomain(BinaryScroogeSerializer.read(is, thrift.Operator))
      }
      Some(ops.toSet)
    } catch {
      case NonFatal(e) =>
        logger.warn(s"Error while decoding library defined in ${directory.relativize(file)}: ${e.getMessage}")
        None
    } finally {
      is.close()
    }
  }

  private def getExtension(file: Path): String = {
    val filename = file.getFileName.toString
    val pos = filename.indexOf('.')
    if (pos > -1) filename.drop(pos + 1) else ""
  }

  private case class Library(ops: Set[Operator], lastModified: Long)

  private object BackgroundThread {
    def apply(): Unit = {
      sleep()
      while (!closed.get) {
        try {
          updateLibraries()
        } catch {
          case NonFatal(e) =>
            // Catch and suppress any other error in order to avoid this thread to die prematurely.
            logger.error("Error while updating libraries", e)
        }
        sleep()
      }
    }

    private def sleep(): Unit = {
      try {
        Thread.sleep(checkFrequency.inMillis)
      } catch {
        case _: InterruptedException =>
          // The thread as been interrupted, but sure the flag has been set property.
          // https://stackoverflow.com/a/4906814
          Thread.currentThread().interrupt()
      }
    }
  }

}
