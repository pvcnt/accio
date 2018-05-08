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
import java.nio.file.{Files, Paths}

import com.twitter.util.StorageUnit
import fr.cnrs.liris.util.{FileUtils, NullInputStream}

import scala.collection.JavaConverters._

object PosixFilesystem extends Filesystem {
  override def createInputStream(uri: String): InputStream = {
    if (Files.isRegularFile(Paths.get(uri))) {
      new BufferedInputStream(new FileInputStream(uri))
    } else {
      NullInputStream
    }
  }

  override def createOutputStream(uri: String): OutputStream = {
    Files.createDirectories(Paths.get(uri).getParent)
    new BufferedOutputStream(new FileOutputStream(uri))
  }

  override def delete(uri: String): Unit = FileUtils.safeDelete(Paths.get(uri))

  override def list(uri: String): Iterator[String] = {
    val path = Paths.get(uri)
    if (Files.isDirectory(path)) {
      Files.list(path).iterator.asScala.map(_.toString)
    } else {
      Iterator.single(uri)
    }
  }

  override def size(uri: String): StorageUnit = StorageUnit.fromBytes(Files.size(Paths.get(uri)))
}