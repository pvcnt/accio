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

package fr.cnrs.liris.accio.ops.io

import java.nio.file.{Files, Paths}

import com.google.common.base.MoreObjects
import fr.cnrs.liris.accio.ops.sparkle.DataSource

import scala.reflect._

/**
 * Data source reading data from our CSV format. There is one CSV file per source key.
 *
 * @param uri     Path to the directory from where to read.
 * @param decoder Decoder to read elements from each CSV file.
 * @tparam T Type of elements being read.
 */
class CsvSource[T: ClassTag](uri: String, decoder: Decoder[T]) extends DataSource[T] {
  private[this] val path = Paths.get(uri)
  require(path.toFile.isDirectory, s"$uri is not a directory")
  require(path.toFile.canRead, s"$uri is unreadable")

  override def keys: Seq[String] = {
    path.toFile
      .listFiles
      .filter(_.getName.endsWith(".csv"))
      .map(_.getName.stripSuffix(".csv"))
      .toSeq
      .sortWith(sort)
  }

  override def read(id: String): Iterable[T] = {
    val bytes = Files.readAllBytes(path.resolve(s"$id.csv"))
    decoder.decode(id, bytes)
  }

  override def toString: String =
    MoreObjects.toStringHelper(this)
      .addValue(classTag[T].runtimeClass.getName)
      .add("uri", uri)
      .toString

  private def sort(key1: String, key2: String): Boolean = {
    val parts1 = key1.split("-").tail.map(_.toInt)
    val parts2 = key2.split("-").tail.map(_.toInt)
    if (parts1.isEmpty && parts2.isEmpty) {
      key1 < key2
    } else if (parts1.isEmpty) {
      true
    } else if (parts2.isEmpty) {
      false
    } else {
      sort(parts1, parts2)
    }
  }

  private def sort(parts1: Seq[Int], parts2: Seq[Int]): Boolean = {
    parts1.zip(parts2).find { case (a, b) => a != b }.exists { case (a, b) => a < b }
  }
}