/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.common.util

/**
 * A family of operating system.
 *
 * @param canonicalName Normalized canonical name.
 * @param detectionName Name used to detect this OS from system properties.
 * @param isUnix        Whether this OS is Unix based.
 */
case class OS(canonicalName: String, detectionName: String, isUnix: Boolean) {
  override def toString: String = canonicalName
}

/**
 * Factory for [[OS]].
 */
object OS {
  val Darwin = OS("osx", "Mac OS X", isUnix = true)
  val FreeBsd = OS("freebsd", "FreeBSD", isUnix = true)
  val Linux = OS("linux", "Linux", isUnix = true)
  val Windows = OS("windows", "Windows", isUnix = false)
  val Unknown = OS("unknown", "", isUnix = false)

  /**
   * Return the version of the current operating system.
   */
  def version: String = sys.props("os.version")

  /**
   * The current operating system.
   */
  lazy val Current: OS = {
    sys.props.get("os.name").flatMap { osName =>
      // Windows have many names, all starting with "Windows".
      Seq(Darwin, FreeBsd, Linux, Windows).find(os => osName.startsWith(os.detectionName))
    }.getOrElse(Unknown)
  }
}