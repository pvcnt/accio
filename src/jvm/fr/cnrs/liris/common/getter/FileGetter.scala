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
import java.nio.file.{Files, Path, Paths}

/**
 *
 * @param copy Whether to copy of use symlinks.
 */
class FileGetter(copy: Boolean) extends Getter {
  override def get(url: URI, dst: Path): Unit = {
    val srcPath = Paths.get(url.getPath)

    // The source path must exist and be a directory to be usable.
    if (!srcPath.toFile.exists()) {
      throw new IOException(s"Source does not exist: ${srcPath.toAbsolutePath}")
    } else if (!srcPath.toFile.isFile) {
      throw new IOException(s"Source must be a file: ${srcPath.toAbsolutePath}")
    }

    // If the destination already exists, it must be a symlink.
    if (dst.toFile.exists()) {
      if (copy) {
        throw new IOException(s"Destination already exists: ${dst.toAbsolutePath}")
      } else if (!Files.isSymbolicLink(dst)) {
        throw new IOException(s"Destination already exists and is not a symlink: ${dst.toAbsolutePath}")
      }
      dst.toFile.delete()
    }

    // Create parent directories.
    Files.createDirectories(dst.getParent)

    if (!copy) {
      Files.createSymbolicLink(dst, srcPath)
    } else {
      Files.copy(srcPath, dst)
    }
  }
}
