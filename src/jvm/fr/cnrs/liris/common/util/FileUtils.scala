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

package fr.cnrs.liris.common.util

import java.io.File
import java.nio.file.{Path, Paths}

/**
 * Helpers dealing with files and paths.
 */
object FileUtils {
  /**
   * Recursively delete anything at a given path, if it exists. It does not produce any error if the path does
   * not exist.
   *
   * @param path Path to delete.
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
        if (file.isDirectory) {
          file.listFiles().foreach(clean)
        }
        file.delete()
      }
      clean(file)
    }
  }

  /**
   * Replace an initial tilde '~' in a URI by the current user's home directory. If a tilde is found elsewhere, it
   * will not be replaced.
   *
   * @param uri URI.
   */
  def replaceHome(uri: String): String =
  if (uri.startsWith("~")) sys.props("user.home") + uri.drop(1) else uri

  def expandPath(uri: String): Path = {
    Paths.get(replaceHome(uri))
  }

  def removeExtension(filename: String): String = {
    val pos = filename.lastIndexOf('.')
    if (pos > 1) filename.substring(0, pos) else filename
  }
}