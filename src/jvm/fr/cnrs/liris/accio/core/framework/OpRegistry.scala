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

import java.util.NoSuchElementException

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.core.api.Operator

/**
 * Stores all operators known to Accio. It is immutable, all operators should be registered when the object is created.
 *
 * @param reader  Operator metadata reader.
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
   * @param name Operator name.
   */
  def contains(name: String): Boolean = index.contains(name)

  /**
   * Return operator metadata for the given operator name, if it exists.
   *
   * @param name Operator name.
   */
  def get(name: String): Option[OpMeta] = index.get(name)

  /**
   * Return operator metadata for the given operator name.
   *
   * @param name Operator name.
   * @throws NoSuchElementException If there is no operator with the given name.
   */
  @throws[NoSuchElementException]
  def apply(name: String): OpMeta = index(name)
}