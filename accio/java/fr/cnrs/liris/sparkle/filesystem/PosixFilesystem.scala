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
import fr.cnrs.liris.util.FileUtils

import scala.collection.JavaConverters._

object PosixFilesystem extends Filesystem {
  override def openRead(uri: String): InputStream = {
    new BufferedInputStream(new FileInputStream(uri))
  }

  override def openWrite(uri: String): OutputStream = {
    new BufferedOutputStream(new FileOutputStream(uri))
  }

  override def delete(uri: String): Unit = FileUtils.safeDelete(Paths.get(uri))

  override def list(uri: String): Iterator[String] = {
    Files.list(Paths.get(uri)).iterator.asScala.map(_.toString)
  }

  override def size(uri: String): StorageUnit = StorageUnit.fromBytes(Files.size(Paths.get(uri)))
}