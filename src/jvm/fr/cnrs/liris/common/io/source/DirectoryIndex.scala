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

package fr.cnrs.liris.common.io.source

import java.io.File
import java.nio.file.Path

class DirectoryIndex(url: String, labelize: Path => Set[String]) extends AbstractIndex {
  private[this] val root = new File(url)
  require(root.exists && root.isDirectory, s"URL must be a valid and readable directory path (got $url)")

  override protected def getFiles: Set[IndexedFile] = read(root)

  protected def read(file: File): Set[IndexedFile] =
    file.listFiles
        .filter(!_.getName.startsWith("."))
        .flatMap(readFile)
        .toSet

  private def readFile(file: File): Set[IndexedFile] = {
    if (file.isDirectory) {
      read(file)
    } else {
      val path = file.toPath.toAbsolutePath
      val labels = labelize(path.subpath(root.toPath.toAbsolutePath.getNameCount, path.getNameCount))
      Set(IndexedFile(path.toString, labels = labels))
    }
  }
}

object DirectoryIndex {
  def apply(url: String): DirectoryIndex = new DirectoryIndex(url, _ => Set.empty)

  def apply(url: String, labelize: Path => Set[String]): DirectoryIndex =
    new DirectoryIndex(url, labelize)
}