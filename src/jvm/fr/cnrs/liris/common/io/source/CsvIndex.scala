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

import java.nio.file.Paths

import scala.io.Source

class CsvIndex(url: String, metaUrl: Option[String], urlParams: Map[String, Any]) extends AbstractIndex {
  private[this] val path = Paths.get(url)

  override protected def getFiles: Set[IndexedFile] = {
    val lines = Source.fromFile(url).getLines()
    if (!lines.hasNext) {
      Set.empty
    } else {
      val columns = lines.next.split(",").map(_.trim).zipWithIndex.toMap
      require(columns.contains("filename"), "Index CSV must have a filename column")
      val invalid = columns.keys.filterNot(col => col.startsWith("label") || Set("filename", "offset", "length").contains(col))
      require(invalid.isEmpty, s"Invalid column names: ${invalid.mkString(", ")}")

      val filenamePos = columns("filename")
      val offsetPos = columns.get("offset")
      val lengthPos = columns.get("length")
      val labelsPos = columns.filter(_._1.startsWith("label")).values.toSeq

      lines.map { line =>
        val parts = line.split(",").map(_.trim)
        require(parts.length > filenamePos)
        val filename = parts(filenamePos)
        val offset = extract(parts, offsetPos).map(_.toInt).getOrElse(0)
        val length = extract(parts, lengthPos).map(_.toInt).getOrElse(0)
        val labels = labelsPos
            .flatMap(pos => extract(parts, Some(pos)))
            .toSet
        IndexedFile(filename, offset, length, labels)
      }.toSet
    }
  }

  override protected val urlSuffix =
    if (urlParams.nonEmpty) urlParams.map { case (k, v) => s"$k=$v" }.mkString("&") else ""

  override protected val urlPrefix = {
    val metaPath = metaUrl.map(Paths.get(_)).getOrElse(path.getParent.resolve(path.getFileName.toString.replace(".csv", "-meta.csv")))
    if (metaPath.toFile.exists) {
      val lines = Source.fromFile(metaPath.toFile).getLines().take(2).toSeq
      if (lines.length >= 2) lines.last else ""
    } else {
      ""
    }
  }

  private def extract(parts: Array[String], maybePos: Option[Int]): Option[String] =
    maybePos.flatMap(pos => if (parts.length > pos) Some(parts(pos)) else None)
}

object CsvIndex {
  def apply(url: String) = new CsvIndex(url, None, Map.empty)

  def apply(url: String, metaUrl: String) = new CsvIndex(url, Some(metaUrl), Map.empty)

  def apply(url: String, metaUrl: String, urlParams: Map[String, Any]) = new CsvIndex(url, Some(metaUrl), urlParams)

  def apply(url: String, urlParams: Map[String, Any]) = new CsvIndex(url, None, urlParams)
}