/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.common.reflect

import scala.reflect.runtime.universe._

/**
 * A prototype is a name associated with a type.
 *
 * @param name A name
 * @param tag  A type
 * @tparam T Prototype type
 */
case class Prototype[T](name: String, tag: TypeTag[T]) {
  /**
   * Representation used in informational messages.
   *
   * @return A string to use in human-readable messages
   */
  def asString: String = s"$name:${tag.tpe.toString}"

  /**
   * Binds this prototype to a given value. The actual prototype is not modified.
   *
   * @param value A value to affect to this prototype
   * @return A new value
   */
  def bind(value: T): Value[T] = Value(this, value)

  override def equals(that: Any): Boolean = that match {
    case proto: Prototype[_] => proto.name == name && proto.tag.tpe =:= tag.tpe
    case _ => false
  }
}

/**
 * A prototype bound to a fixed (= already evaluated) value.
 *
 * @param proto A prototype
 * @param value A value
 * @tparam T Prototype type
 */
final case class Value[T](proto: Prototype[T], value: T) {
  /**
   * Representation used in informational messages.
   *
   * @return A string to use in human-readable messages
   */
  def asString: String = s"${proto.asString}=${value.toString}"
}