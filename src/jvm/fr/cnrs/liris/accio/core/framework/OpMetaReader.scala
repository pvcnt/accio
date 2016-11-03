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

package fr.cnrs.liris.accio.core.framework

import fr.cnrs.liris.accio.core.api.Operator

/**
 * Metadata about an operator, i.e., its definition plus runtime information.
 *
 * @param defn     Operator definition.
 * @param opClass  Operator class.
 * @param inClass  Operator's input arguments class.
 * @param outClass Operator's output arguments class.
 */
case class OpMeta(defn: OpDef, opClass: Class[_ <: Operator[_, _]], inClass: Option[Class[_]], outClass: Option[Class[_]])

/**
 * Exception thrown when the definition of an operator is invalid.
 *
 * @param clazz Operator class.
 * @param cause Root exception.
 */
class IllegalOpException(clazz: Class[_ <: Operator[_, _]], cause: Throwable)
  extends Exception(s"Illegal definition of operator ${clazz.getName}: ${cause.getMessage}", cause)

/**
 * A metadata reader extracts metadata about operators.
 */
trait OpMetaReader {
  /**
   * Read operator metadata from its class specification.
   *
   * @param clazz Operator class.
   * @return Operator metadata.
   * @throws IllegalOpException If the operator definition is invalid.
   */
  @throws[IllegalOpException]
  def read[T <: Operator[_, _]](clazz: Class[T]): OpMeta
}