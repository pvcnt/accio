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

package fr.cnrs.liris.accio.core.service

import com.google.inject.{Inject, Injector, ProvisionException}
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.api.Operator
import fr.cnrs.liris.accio.core.domain.OpDef

/**
 * Factory for [[Operator]].
 *
 * Note: Injecting an [[Injector]] is generally considered a bad practice, but it is really required here.
 * Although the implementation is very short and trivial, this class has the benefit of keeping the number of places
 * where the [[Injector]] is injected small.
 *
 * @param opRegistry Runtime operator registry.
 * @param injector   Guice injector.
 */
final class OpFactory @Inject()(opRegistry: RuntimeOpRegistry, injector: Injector) extends LazyLogging {
  /**
   * Create a new operator.
   *
   * @param opDef Operator definition.
   * @throws IllegalArgumentException If there is no operator with given name.
   * @throws ProvisionException       If an error happens during operator instantiation.
   */
  @throws[ProvisionException]
  @throws[IllegalArgumentException]
  def create(opDef: OpDef): Operator[_, _] = {
    opDef.deprecation.foreach { deprecation =>
      logger.warn(s"Using a deprecated operator ${opDef.name}: $deprecation")
    }
    opRegistry.getMeta(opDef.name) match {
      case None => throw new IllegalArgumentException(s"No runtime information about operator: ${opDef.name}")
      case Some(opMeta) => injector.getInstance(opMeta.opClass)
    }
  }
}