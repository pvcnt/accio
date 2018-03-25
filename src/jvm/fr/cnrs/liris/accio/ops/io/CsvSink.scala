/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.ops.io

import java.nio.file.{Files, Paths}

import com.google.common.base.MoreObjects
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.ops.sparkle.DataSink

import scala.reflect._

/**
 * Data sink writing data in our CSV format. There is one CSV file per source key.
 *
 * @param uri                     Path to the directory where to write.
 * @param encoder                 Encoder to write elements into each CSV file.
 * @param failOnNonEmptyDirectory Whether to fail is specified directory exists and is not empty.
 * @tparam T Type of elements being written.
 */
class CsvSink[T: ClassTag](uri: String, encoder: Encoder[T], failOnNonEmptyDirectory: Boolean = true)
  extends DataSink[T] with LazyLogging {

  private[this] lazy val path = {
    val path = Paths.get(uri)
    if (!path.toFile.exists) {
      Files.createDirectories(path)
    } else if (path.toFile.isDirectory && path.toFile.listFiles.nonEmpty && failOnNonEmptyDirectory) {
      throw new IllegalArgumentException(s"Non-empty directory: ${path.toAbsolutePath}")
    } else if (path.toFile.isFile) {
      throw new IllegalArgumentException(s"${path.toAbsolutePath} already exists and is a file")
    }
    path
  }

  override def write(key: String, elements: Seq[T]): Unit = {
    if (elements.nonEmpty) {
      val bytes = encoder.encode(key, elements)
      Files.write(path.resolve(s"$key.csv"), bytes)
    }
  }

  override def toString: String =
    MoreObjects.toStringHelper(this)
      .addValue(classTag[T].runtimeClass.getName)
      .add("uri", uri)
      .toString
}