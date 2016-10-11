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

import com.google.inject.{Inject, Singleton}

/**
 * The operator registry stores all operators known to Accio. It is immutable, all operators should be
 * registered when the object is created.
 *
 * @param reader  Operator definition reader.
 * @param classes Classes containing operator implementations.
 */
@Singleton
class OpRegistry @Inject()(reader: OpMetaReader, classes: Set[Class[_ <: Operator[_, _]]]) {
  private[this] val index = classes.map { clazz =>
    val meta = reader.read(clazz)
    meta.defn.name -> meta
  }.toMap

  /**
   * Return all operators known to this registry.
   */
  def ops: Set[OpMeta] = index.values.toSet

  /**
   * Check whether the registry contains an operator with given name.
   *
   * @param name Operator name
   */
  def contains(name: String): Boolean = index.contains(name)

  /**
   * Return operator definition for the given operator name, if it exists.
   *
   * @param name Operator name
   */
  def get(name: String): Option[OpMeta] = index.get(name)

  /**
   * Return operator definition for the given operator name.
   *
   * @param name Operator name
   * @throws NoSuchElementException If there is no operator for the given name
   */
  @throws[NoSuchElementException]
  def apply(name: String): OpMeta = index(name)
}