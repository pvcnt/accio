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

/**
 * A family of operating system.
 *
 * @param canonicalName Normalized canonical name.
 * @param detectionName Name used to detect this OS from system properties.
 */
case class OS(canonicalName: String, detectionName: String) {
  override def toString: String = canonicalName
}

/**
 * Factory for [[OS]].
 */
object OS {
  val Darwin = OS("osx", "Mac OS X")
  val FreeBsd = OS("freebsd", "FreeBSD")
  val Linux = OS("linux", "Linux")
  val Windows = OS("windows", "Windows")
  val Unknown = OS("unknown", "")

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