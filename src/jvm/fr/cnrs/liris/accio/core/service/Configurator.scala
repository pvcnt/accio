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

package fr.cnrs.liris.accio.core.service

import com.typesafe.scalalogging.StrictLogging

class Configurator(configs: Set[_]) extends StrictLogging {
  private[this] val index: Map[Class[_], _] = configs.map(config => config.getClass -> config).toMap

  def initialize(objs: Any*): Unit =
    objs.foreach {
      case obj: Configurable[_] =>
        index.get(obj.configClass) match {
          case None => logger.error(s"No configuration available for ${obj.getClass.getName}")
          case Some(config) =>
            initialize(obj, config)
            logger.info(s"Initialized ${obj.getClass.getSimpleName}: $config")
        }
      case _ => // Nothing to do.
    }

  private def initialize[T](obj: Configurable[_], config: T) = {
    obj.asInstanceOf[Configurable[T]].initialize(config)
  }
}

object Configurator {
  def apply(configs: Any*): Configurator = new Configurator(configs.toSet)
}