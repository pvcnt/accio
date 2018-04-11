/*
 * Accio is a platform to launch computer science experiments.
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

package fr.cnrs.liris.util.reflect

/**
 * A high-level interface to manipulate case classes fields with the Scala reflection API.
 *
 * @param name         Field name.
 * @param defaultValue A default value for this field, if any.
 * @param annotations  A list of runtime annotations applied on this field.
 * @param scalaType    Type of this field.
 */
final class CaseClassField private[reflect](
  val name: String,
  val index: Int,
  val defaultValue: Option[_],
  val annotations: AnnotationList,
  val scalaType: ScalaType) {

  /**
   * Return the associated JVM runtime class.
   */
  def runtimeClass: Class[_] = scalaType.runtimeClass
}
