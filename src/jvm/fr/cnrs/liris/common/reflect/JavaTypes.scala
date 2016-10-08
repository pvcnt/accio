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

package fr.cnrs.liris.common.reflect

object JavaTypes {
  private[this] val javaToScalaTypes = Map[Class[_], Class[_]](
    classOf[java.lang.Boolean] -> classOf[Boolean],
    classOf[java.lang.Byte] -> classOf[Byte],
    classOf[java.lang.Short] -> classOf[Short],
    classOf[java.lang.Integer] -> classOf[Int],
    classOf[java.lang.Long] -> classOf[Long],
    classOf[java.lang.Double] -> classOf[Double])

  def asScala(clazz: Class[_]): Class[_] = javaToScalaTypes.getOrElse(clazz, clazz)

  def maybeAsScala(clazz: Class[_]): Option[Class[_]] = javaToScalaTypes.get(clazz)
}
