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

package fr.cnrs.liris.accio.core.util

/**
 * Trait for objects that are configured after being instantiated. Configuration is done via a custom object, normally
 * a case class. The rationale for this is to differentiate between dependencies that are statically injected via the
 * constructor, with Guice, and configuration parameters that can be specified at runtime. Although we could inject
 * those parameters too (e.g., with Guice binding annotations), it did not seem the right pattern.
 *
 * Developers note: you must not use `config` inside the constructor, as the configuration will not be available at
 * this time, thus resulting in an exception. If you absolutely have to use `config` inside a constructor, use lazy
 * val's to delay evaluation afterwards, when configuration will be available.
 *
 * @tparam T Configuration type.
 */
trait Configurable[T] {
  private[this] var _config: Option[T] = None

  def configClass: Class[T]

  /**
   * Initialize this instance with a given config.
   *
   * @param config Configuration object.
   * @return Current instance.
   */
  final def initialize(config: T): this.type = {
    _config = Some(config)
    this
  }

  /**
   * Return current configuration object.
   *
   * @throws IllegalStateException If this method is called before this instance has been initialized.
   */
  final protected def config: T = _config match {
    case None => throw new IllegalStateException(s"${getClass.getName} has not been initialized")
    case Some(config) => config
  }
}