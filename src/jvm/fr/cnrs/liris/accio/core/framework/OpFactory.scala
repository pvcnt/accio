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

import com.google.inject.{Inject, Injector, ProvisionException}
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.api.Operator

/**
 * Factory for [[Operator]]. The implementation is very short and trivial, however it makes the purpose clearer and
 * more consistent with other factories. Moreover, as injecting [[Injector]]s is considered a bad practice, it avoids
 * injecting it in a larger class with much more logic.
 *
 * @param injector Guice injector.
 */
final class OpFactory @Inject()(injector: Injector) extends LazyLogging {
  /**
   * Create a new operator.
   *
   * @param opMeta Operator metadata.
   * @throws ProvisionException If an error happens during operator instantiation.
   */
  @throws[ProvisionException]
  def create(opMeta: OpMeta): Operator[_, _] = {
    opMeta.defn.deprecation.foreach { deprecation =>
      logger.warn(s"Using a deprecated operator ${opMeta.defn.name}: $deprecation")
    }
    injector.getInstance(opMeta.opClass)
  }
}
