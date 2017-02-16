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

package fr.cnrs.liris.accio.core.version

/**
 * Describes a version using semantic versioning.
 *
 * @param major Major component.
 * @param minor Minor component.
 * @param patch Patch component.
 * @param label Additional label.
 */
case class Version private(major: Int, minor: Int, patch: Int, label: Option[String]) {
  override def toString: String = s"$major.$minor.$patch${label.map(str => s"-$str").getOrElse("")}"
}

/**
 * Factory for [[Version]].
 */
object Version {
  /**
   * Return the current version of Accio.
   */
  val Current = Version(2, 0, 0, Some("dev"))
}