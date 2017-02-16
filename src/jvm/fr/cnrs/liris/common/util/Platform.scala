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
import scala.util.control.NonFatal

/**
 * Provide some system level information.
 */
object Platform {
  /**
   * Return total RAM memory mounted on the machine, if possible to obtain. Works only on Unix platforms.
   */
  lazy val totalMemory: Option[StorageUnit] = {
    if (OS.Current == OS.Linux || OS.Current == OS.FreeBsd) {
      readMeminfo("MemTotal")
    } else if (OS.Current == OS.Darwin) {
      readSysctl("hw.memsize")
    } else {
      None
    }
  }

  /**
   * Return free RAM memory on the machine, if possible to obtain. Works only on Unix platforms.
   */
  def freeMemory: Option[StorageUnit] = {
    if (OS.Current == OS.Linux || OS.Current == OS.FreeBsd) {
      readMeminfo("MemFree")
    } else {
      None
    }
  }

  /**
   * Return available RAM memory on the machine, if possible to obtain. Works only on Unix platforms.
   */
  def availableMemory: Option[StorageUnit] = {
    if (OS.Current == OS.Linux || OS.Current == OS.FreeBsd) {
      readMeminfo("MemAvailable")
    } else {
      None
    }
  }

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
   * Read a key from the meminfo util, if available. Works only on Linux and FreeBSD platforms.
   *
   * @param key Key to read.
   */
  private def readMeminfo(key: String): Option[StorageUnit] = {
    if (OS.Current == OS.Linux || OS.Current == OS.FreeBsd) {
      try {
        Process("cat /proc/meminfo")
          .lineStream
          .find(_.startsWith(s"$key:"))
          .map { line =>
            val kiloBytes = line.stripPrefix(s"$key:").trim.stripSuffix(" kB").toLong
            StorageUnit.fromKilobytes(kiloBytes)
          }
      } catch {
        case NonFatal(_) => None
      }
    } else {
      None
    }
  }

  /**
   * Read a key from the sysctl util, if available. Works only on Darwin platforms.
   *
   * @param key Key to read.
   */
  private def readSysctl(key: String): Option[StorageUnit] = {
    if (OS.Current == OS.Darwin) {
      try {
        val line = Process(s"sysctl $key").lineStream.head
        val bytes = line.stripPrefix(s"$key:").trim.toLong
        Some(StorageUnit.fromBytes(bytes))
      } catch {
        case NonFatal(_) => None
      }
    } else {
      None
    }
  }
}