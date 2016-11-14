/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.common.util

import java.io.File
import java.nio.file.Path

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

  def removeExtension(filename: String): String = {
    val pos = filename.lastIndexOf('.')
    if (pos > 1) filename.substring(0, pos) else filename
  }
}