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

package fr.cnrs.liris.sparkle.filesystem

import java.io._
import java.net.URI
import java.nio.file.{Files, Path, Paths}

import fr.cnrs.liris.util.{FileUtils, NullInputStream}

import scala.collection.JavaConverters._

/**
 * Default implementation of a filesystem, relying on Java NIO to implement all operations.
 *
 * This allows to support even non-POSIX filesystems, as long as the associated
 * [[java.nio.file.spi.FileSystemProvider]] class is registered as a service.
 */
object DefaultFilesystem extends Filesystem {
  override def createInputStream(uri: String): InputStream = {
    val file = getFile(uri)
    if (Files.isRegularFile(file)) {
      new BufferedInputStream(new FileInputStream(file.toFile))
    } else {
      NullInputStream
    }
  }

  override def createOutputStream(uri: String): OutputStream = {
    val file = getFile(uri)
    // We first create the parent directory and the file. That way, even if no content at all is
    // written, the file will still exist.
    Files.createDirectories(file.getParent)
    Files.createFile(file)
    new BufferedOutputStream(new FileOutputStream(file.toFile))
  }

  override def delete(uri: String): Unit = FileUtils.safeDelete(getFile(uri))

  override def list(uri: String): Iterable[String] = {
    def enumerate(file: Path): Seq[String] = {
      if (Files.isDirectory(file) && !Files.isHidden(file)) {
        Files.list(file).iterator.asScala.flatMap(enumerate).toSeq
      } else {
        Seq(file.toUri.toString)
      }
    }

    enumerate(getFile(uri))
  }

  override def isDirectory(uri: String): Boolean = Files.isDirectory(getFile(uri))

  override def isFile(uri: String): Boolean = Files.isRegularFile(getFile(uri))

  private def getFile(uri: String): Path = {
    if (uri.startsWith("file:")) {
      // Paths.get does not accept URIs with an authority, even empty. However NIO APIs
      // (e.g., Files.list) generate URIs with such empty authorities...
      Paths.get(new URI(uri.replace("://", ":")))
    } else {
      // We accept, for this filesystem only, a raw path to be passed as argument.
      Paths.get(uri)
    }
  }
}