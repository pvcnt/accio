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

package fr.cnrs.liris.accio.core.application

/**
 * Trait for objects that are configured after being instantiated. Configuration is done via a custom object, normally
 * a case class.
 *
 * Rationale: usually, configuration is injected through constructors. However, it causes a mix between configuration
 * parameters and dependencies in constructors. While dependencies are usually statically wired (e.g., via Guice
 * modules), configuration parameters tend to be statically defined at runtime by users. Thus, there is no easy way
 * to automatically inject them when using a DI framework and [[javax.inject.Inject]] annotations. With
 * [[Configurable]] classes, only dependencies are injected through constructors, while configuration parameters are
 * set by calling the `initialize` method. This method should be called as soon as possible after
 * object creation, ideally automatically if the DI framework allows it.
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

  def initialize(config: T): Unit = {
    _config = Some(config)
  }

  protected def config: T = _config match {
    case None => throw new IllegalStateException(s"${getClass.getName} has not been initialized")
    case Some(config) => config
  }
}