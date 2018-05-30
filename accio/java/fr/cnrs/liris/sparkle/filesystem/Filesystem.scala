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

import java.io.{InputStream, OutputStream}

import com.twitter.util.StorageUnit

trait Filesystem {
  /**
   * Create an input stream pointing to a given file.
   *
   * @param uri URI to a file. The URI should match this filesystem's scheme.
   */
  def createInputStream(uri: String): InputStream

  /**
   * Create an output stream pointing to a given file.
   *
   * @param uri URI to a file. The URI should match this filesystem's scheme.
   */
  def createOutputStream(uri: String): OutputStream

  /**
   * Delete a resource. It should handle files and directories (in which case they are deleted
   * with all of their contained files and directories).
   *
   * @param uri URI to a file or directory. The URI should match this filesystem's scheme.
   */
  def delete(uri: String): Unit

  /**
   * List the files contained inside a given directory. Files are recursively listed (i.e.,
   * including sub-directories at any level), and are returned as proper URIs, ready to be used in
   * another method of this class.
   *
   * @param uri URI to a directory. The URI should match this filesystem's scheme.
   */
  def list(uri: String): Iterable[String]

  /**
   * Return whether a resource is a directory.
   *
   * @param uri URI to a file or directory. The URI should match this filesystem's scheme.
   */
  def isDirectory(uri: String): Boolean

  /**
   * Return whether a resource is a regular file.
   *
   * @param uri URI to a file or directory. The URI should match this filesystem's scheme.
   */
  def isFile(uri: String): Boolean
}