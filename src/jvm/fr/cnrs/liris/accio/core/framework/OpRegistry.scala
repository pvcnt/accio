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

package fr.cnrs.liris.accio.core.framework

import java.util.NoSuchElementException

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * Operator registry stores all operators known to Accio.
 */
class OpRegistry(metaReader: OpMetaReader) {
  private[this] val _ops = mutable.Map.empty[String, OpMeta]

  /**
   * Register a new operator.
   *
   * @tparam T Operator type
   * @throws IllegalOpDefinition      If the operator definition is invalid
   * @throws IllegalArgumentException If an operator with the same name is already registered
   * @return Operator metadata
   */
  @throws[IllegalOpDefinition]
  @throws[IllegalArgumentException]
  def register[T <: Operator : ClassTag : TypeTag]: OpMeta = {
    val meta = metaReader.read[T]
    require(!_ops.contains(meta.defn.name), s"Duplicate operator ${meta.defn.name}")
    _ops(meta.defn.name) = meta
    meta
  }

  /**
   * Return all registered operators, ordered by name.
   */
  def ops: Seq[OpMeta] = _ops.values.toSeq.sortBy(_.defn.name)

  /**
   * Check whether the registry contains an [[Operator]] for the given name.
   *
   * @param name An operation name
   * @return True if there is an operation for the given name, false otherwise
   */
  def contains(name: String): Boolean = _ops.contains(name)

  /**
   * Return the [[Operator]] class for the given name, if it exists.
   *
   * @param name An operation name
   */
  def get(name: String): Option[OpMeta] = _ops.get(name)

  /**
   * Return the [[Operator]] class for the given name.
   *
   * @param name An operation name
   * @throws NoSuchElementException If there is no operation for the given name
   */
  @throws[NoSuchElementException]
  def apply(name: String): OpMeta = _ops(name)
}