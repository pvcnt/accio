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

package fr.cnrs.liris.accio.framework.filesystem.posix

import java.io.IOException
import java.nio.file.{Files, Path}

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.framework.filesystem.FileSystem
import fr.cnrs.liris.common.util.FileUtils

/**
 * POSIX filesystem.
 *
 * @param path    Root directory under which files are stored.
 * @param symlink Whether to create symlinks when reading files (only), instead of copying them.
 */
@Singleton
private[posix] final class PosixFileSystem @Inject()(@PosixPath path: Path, @PosixSymlink symlink: Boolean) extends FileSystem {
  override def write(src: Path, filename: String): String = {
    // We copy files to target path. We do *not* want to symlink them, as original files can disappear at any time.
    //TODO: prevent from going up in the hierarchy.
    val dst = path.resolve(filename)
    Files.createDirectories(dst.getParent)
    FileUtils.recursiveCopy(src, dst)
    dst.toAbsolutePath.toString
  }

  override def read(filename: String, dst: Path): Unit = {
    val src = path.resolve(filename)

    // The source path must exist and be a directory to be usable.
    if (!src.toFile.exists()) {
      throw new IOException(s"Source does not exist: ${src.toAbsolutePath}")
    }

    // If the destination already exists, it must be a symlink.
    if (dst.toFile.exists()) {
      if (!symlink) {
        throw new IOException(s"Destination already exists: ${dst.toAbsolutePath}")
      } else if (!Files.isSymbolicLink(dst)) {
        throw new IOException(s"Destination already exists and is not a symlink: ${dst.toAbsolutePath}")
      }
      dst.toFile.delete()
    }

    // Create parent directories.
    Files.createDirectories(dst.getParent)

    if (symlink) {
      Files.createSymbolicLink(dst, src)
    } else {
      FileUtils.recursiveCopy(src, dst)
    }
  }

  override def delete(filename: String): Unit = {
    FileUtils.safeDelete(path.resolve(filename))
  }
}