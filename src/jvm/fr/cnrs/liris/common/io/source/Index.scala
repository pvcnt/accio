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

case class IndexedFile(url: String, offset: Long = 0, length: Long = 0, labels: Set[String] = Set.empty)

trait Index extends Serializable {
  /**
   * Return all available labels.
   */
  def labels: Seq[String]

  /**
   * Read the index and return the files matching the given label.
   *
   * @param label An optional label the files must have
   */
  def read(label: Option[String]): Seq[IndexedFile]
}

/**
 * Base implementation of an index.
 *
 */
abstract class AbstractIndex extends Index {
  private[this] lazy val sortedFiles: Map[String, Seq[IndexedFile]] =
    getFiles
        .flatMap(file => file.labels.map(lbl => lbl -> file) ++ Seq("*" -> file))
        .groupBy(_._1)
        .map { case (label, files) => label -> files.map(_._2).toSeq.sortWith(sortFiles) }

  override lazy val labels: Seq[String] = sortedFiles.keySet.filterNot(_ == "*").toSeq.sorted

  override def read(label: Option[String]): Seq[IndexedFile] = {
    val res = sortedFiles.getOrElse(label.getOrElse("*"), Seq.empty)
    if (urlPrefix.nonEmpty || urlSuffix.nonEmpty) {
      res.map(withSuffixAndPrefix)
    } else {
      res
    }
  }

  /**
   * Return the files to be included inside the index.
   *
   * @return
   */
  protected def getFiles: Set[IndexedFile]

  protected def urlSuffix: String = ""

  protected def urlPrefix: String = ""

  private def sortFiles(a: IndexedFile, b: IndexedFile) =
    if (a.url == b.url) {
      if (a.offset > 0 && a.offset != b.offset) {
        a.offset < b.offset
      } else if (a.length > 0) {
        a.length < b.length
      } else {
        false
      }
    } else {
      a.url < b.url
    }

  private def withSuffixAndPrefix(file: IndexedFile) = {
    var url = file.url
    if (urlPrefix.nonEmpty) {
      url = s"$urlPrefix/$url"
    }
    if (urlSuffix.nonEmpty) {
      url = s"$url?$urlSuffix"
    }
    file.copy(url = url)
  }
}

class InMemoryIndex(files: Iterable[IndexedFile]) extends AbstractIndex {
  override protected def getFiles: Set[IndexedFile] = files.toSet
}