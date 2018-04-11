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

import scala.reflect.runtime.{universe => ru}

// TODO: `tpe` should ultimately be private.
final class ScalaType(val tpe: ru.Type) {
  /**
   * Return the associated JVM runtime class.
   */
  lazy val runtimeClass: Class[_] = ReflectUtils.classForType(tpe)

  def isA[T: ru.TypeTag]: Boolean = tpe =:= ru.typeOf[T]

  def isLike[T: ru.TypeTag]: Boolean = tpe <:< ru.typeOf[T]

  def args: Seq[ScalaType] = tpe.typeArgs.map(new ScalaType(_))

  def baseType[T: ru.TypeTag]: ScalaType = new ScalaType(tpe.baseType(ru.typeOf[T].typeSymbol))

  /**
   * Return whether this field is an optional field.
   */
  def isOption: Boolean = tpe <:< ScalaType.OPTION

  def isUnit: Boolean = tpe =:= ScalaType.UNIT

  override def toString: String = tpe.toString

  override def hashCode: Int = tpe.hashCode()

  override def equals(obj: scala.Any): Boolean =
    obj match {
      case scalaType: ScalaType => scalaType.tpe =:= tpe
      case _ => false
    }
}

object ScalaType {
  private[reflect] val OPTION = ru.typeOf[Option[_]]
  private[reflect] val UNIT = ru.typeOf[Unit]
}
