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

package fr.cnrs.liris.common.util

import java.io.{File, IOException}
import java.nio.file.{Files, Path, Paths}

/**
 * Helpers dealing with files and paths.
 */
object FileUtils {
  /**
   * Recursively delete anything at a given path, if it exists. It does not produce any error if the path does
   * not exist.
   *
   * @param path File or directory to delete.
   */
  def safeDelete(path: Path): Unit = safeDelete(path.toFile)

  /**
   * Delete a file a recursively delete a directory, if it exists. It does not produce any error if the file or
   * directory does not exist.
   *
   * @param file File or directory to delete.
   */
  def safeDelete(file: File): Unit = {
    if (file.exists()) {
      def clean(file: File): Unit = {
        // We do not want to follow symlinks there, otherwise it would delete the content of the target directory!
        if (file.isDirectory && !Files.isSymbolicLink(file.toPath)) {
          file.listFiles().foreach(clean)
        }
        file.delete()
      }

      clean(file)
    }
  }

  /**
   * Expand an URI to replace special directories like in standard bash. A leading tilde '~' will be replaced by
   * user's home directory and a point-slash './' will be replaced by working directory.
   *
   * @param uri URI.
   * @return Expanded URI.
   */
  def expand(uri: String): String = {
    if (uri.startsWith("~/")) {
      sys.props("user.home") + uri.drop(1)
    } else if (uri.startsWith("./")) {
      sys.props("user.dir") + uri.drop(1)
    } else {
      uri
    }
  }

  /**
   * Expand an URI to replace special directories like in standard bash. A leading tilde '~' will be replaced by
   * user's home directory and a point-slash './' will be replaced by working directory.
   *
   * @param uri URI.
   * @return Path to expanded URI.
   */
  def expandPath(uri: String): Path = Paths.get(expand(uri))

  /**
   * Recursively copy the content of a directory into another directory.
   *
   * @param src  Source directory to copy (must exist).
   * @param dest Destination directory where to copy (must not exist).
   */
  def recursiveCopy(src: File, dest: File): Unit = recursiveCopy(src.toPath, dest.toPath)

  /**
   * Recursively copy the content of a directory into another directory.
   *
   * @param src  Source directory to copy (must exist).
   * @param dest Destination directory where to copy (must not exist).
   */
  def recursiveCopy(src: Path, dest: Path): Unit = {
    if (!src.toFile.exists()) {
      throw new IOException(s"Source ${src.toAbsolutePath} does not exist")
    }
    if (!dest.getParent.toFile.canWrite) {
      throw new IOException(s"Destination parent ${dest.toAbsolutePath} is not writable")
    }
    Files.createDirectories(dest.getParent)
    doRecursiveCopy(src, dest)
  }

  private def doRecursiveCopy(src: Path, dest: Path): Unit = {
    require(!dest.toFile.exists(), s"Destination ${dest.toAbsolutePath} already exists")
    if (src.toFile.isFile) {
      Files.copy(src, dest)
    } else {
      Files.createDirectory(dest)
      src.toFile.listFiles.foreach(file => doRecursiveCopy(file.toPath, dest.resolve(file.getName)))
    }
  }
}