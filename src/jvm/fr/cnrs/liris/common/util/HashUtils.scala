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

import java.nio.file.{Path, Paths}
import java.util.zip.CRC32

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.google.common.io.Files

/**
 * Some utilities related to hashing.
 */
object HashUtils {
  /**
   * Get the sha1 hash of a string.
   *
   * @param str A string to hash
   * @return A hash, as a string
   */
  def sha1(str: String): String = Hashing.sha1().hashString(str, Charsets.UTF_8).toString

  /**
   * Get the (sha1) hash of several strings, in an order-independant manner.
   *
   * @param strs Strings to hash
   * @return A hash, as a string
   */
  def sha1(strs: Iterable[String]): String = sha1(strs.toSeq.sorted.mkString("|"))

  def md5(str: String): String = Hashing.md5().hashString(str, Charsets.UTF_8).toString

  def md5Path(path: Path): String =
    if (path.toFile.isFile) {
      Files.hash(path.toFile, Hashing.md5()).toString
    } else {
      md5Paths(path.toFile.listFiles.map(f => Paths.get(f.getAbsolutePath)))
    }

  def md5Paths(paths: Iterable[Path]): String =
    md5(paths.toSeq.sortBy(_.toAbsolutePath.toString).map(md5Path).mkString("|"))

  def crc32(str: String): Long = crc32(str.getBytes)

  def crc32(bytes: Array[Byte]): Long = {
    val hash = new CRC32
    hash.update(bytes)
    hash.getValue
  }

  def maskedCrc32(bytes: Array[Byte]): Long = maskCrc(crc32(bytes))

  def maskedCrc32(str: String): Long = maskCrc(crc32(str))

  private def maskCrc(crc: Long) = ((crc >> 15) | (crc << 17)) + 0xa282ead8l
}