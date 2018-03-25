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

package fr.cnrs.liris.accio.discovery

import fr.cnrs.liris.accio.api.thrift.OpDef
import fr.cnrs.liris.accio.sdk.Operator

/**
 * Metadata about an operator, i.e., its definition and runtime information.
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
 * @param clazz   Operator class.
 * @param message Error message.
 */
class InvalidOpDefException(val clazz: Class[_ <: Operator[_, _]], message: String, cause: Throwable = null)
  extends Exception(s"Illegal definition of operator ${clazz.getName}: $message", cause)

/**
 * Operator discovery service. It is responsible for finding available operators and their definition.
 */
trait OpDiscovery {
  /**
   * Extracts operator metadata from its class specification. This operation can be extensive, its results should
   * be cached afterwards.
   *
   * @param clazz Operator class.
   * @throws InvalidOpDefException If the operator definition is invalid.
   */
  @throws[InvalidOpDefException]
  def read[T <: Operator[_, _]](clazz: Class[T]): OpMeta

  /**
   * Find all available operator implementations. This operation can be extensive, its results should be
   * cached afterwards.
   */
  def discover: Set[Class[_ <: Operator[_, _]]]
}