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

package fr.cnrs.liris.privamov.core.io

import java.nio.file.{Files, Paths}

/**
 * A data source generating one key per file inside a given directory. It is *not* recursive.
 *
 * @param url       Path to the directory.
 * @param extension Only consider files with this extension.
 * @param decoder   Decoder to apply on each file.
 * @tparam T Elements' type.
 */
class DirectorySource[T](url: String, extension: String, decoder: Decoder[T]) extends DataSource[T] {
  private[this] val path = Paths.get(url)
  require(path.toFile.isDirectory, s"Not a directory: $url")

  override final def keys: Seq[String] =
    path.toFile
      .listFiles
      .filter(_.getName.endsWith(extension))
      .map(_.toPath.getFileName.toString.dropRight(extension.length))

  override final def read(key: String): Option[T] =
    decoder.decode(key, Files.readAllBytes(path.resolve(s"$key$extension")))
}