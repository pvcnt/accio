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

import java.io.File

import com.twitter.util.StorageUnit

import scala.sys.process.Process

/**
 * Provide some system level information.
 */
object Platform {
  /**
   * Return total RAM memory mounted on the machine, if possible to obtain. Works only on Unix platforms.
   */
  lazy val totalMemory: Option[StorageUnit] = readMeminfo("MemTotal")

  /**
   * Return free RAM memory on the machine, if possible to obtain. Works only on Unix platforms.
   */
  def freeMemory: Option[StorageUnit] = readMeminfo("MemFree")

  /**
   * Return available RAM memory on the machine, if possible to obtain. Works only on Unix platforms.
   */
  def availableMemory: Option[StorageUnit] = readMeminfo("MemAvailable")

  /**
   * Return total disk space of the root partition.
   */
  lazy val totalDiskSpace: Option[StorageUnit] = {
    val bytes = new File("/").getTotalSpace
    if (bytes > 0) Some(StorageUnit.fromBytes(bytes)) else None
  }

  /**
   * Return free disk space of the root partition.
   */
  def freeDiskSpace: Option[StorageUnit] = {
    val bytes = new File("/").getFreeSpace
    if (bytes > 0) Some(StorageUnit.fromBytes(bytes)) else None
  }

  /**
   * Read a key from the meminfo util, if available. Works only on Unix platforms.
   *
   * @param key Key to read.
   */
  private def readMeminfo(key: String): Option[StorageUnit] = {
    if (OS.Current.isUnix) {
      Process("cat /proc/meminfo").lineStream
        .find(_.startsWith(s"$key:"))
        .map { line =>
          val kiloBytes = line.stripPrefix(s"$key:").stripSuffix(" kB").trim.toLong
          StorageUnit.fromKilobytes(kiloBytes)
        }
    } else {
      None
    }
  }
}