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

package fr.cnrs.liris.common.getter

import java.io.IOException
import java.net.URI
import java.nio.file.{Files, Path}

class HttpGetter extends Getter {
  override def get(src: URI, dst: Path): Unit = {
    // Destination must not already exist.
    if (dst.toFile.exists()) {
      throw new IOException(s"Destination already exists: ${dst.toAbsolutePath}")
    }

    // Create parent directories.
    Files.createDirectories(dst.getParent)

    val os = src.toURL.openStream()
    try {
      Files.copy(os, dst)
    } finally {
      os.close()
    }
  }

  override def schemes: Set[String] = Set("http", "https")
}