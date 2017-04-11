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

package fr.cnrs.liris.accio.core.framework

import fr.cnrs.liris.accio.core.api.{InvalidSpecException, InvalidSpecMessage}

import scala.collection.mutable

private[core] trait BaseFactory {
  protected def doValidate(f: mutable.Set[InvalidSpecMessage] => Unit): ValidationResult = {
    val warnings = mutable.Set.empty[InvalidSpecMessage]
    try {
      f(warnings)
      ValidationResult(warnings.toSeq, Seq.empty)
    } catch {
      case e: InvalidSpecException => ValidationResult(e.errors, e.warnings ++ warnings)
    }
  }

  protected def newError(message: String, warnings: mutable.Set[InvalidSpecMessage]) =
    new InvalidSpecException(Seq(InvalidSpecMessage(message)), warnings.toSeq)

  protected def newError(message: String, path: String, warnings: mutable.Set[InvalidSpecMessage]) =
    new InvalidSpecException(Seq(InvalidSpecMessage(message, Some(path))), warnings.toSeq)

  protected def newError(messages: Iterable[String], warnings: mutable.Set[InvalidSpecMessage]) =
    new InvalidSpecException(messages.map(InvalidSpecMessage(_)).toSeq, warnings.toSeq)

  protected def newError(message: String, paths: Iterable[String], warnings: mutable.Set[InvalidSpecMessage]) =
    new InvalidSpecException(paths.map(path => InvalidSpecMessage(message, Some(path))).toSeq, warnings.toSeq)

  protected def newError(messages: Iterable[String], paths: Iterable[String], warnings: mutable.Set[InvalidSpecMessage]) =
    new InvalidSpecException(messages.zip(paths).map { case (message, path) => InvalidSpecMessage(message, Some(path)) }.toSeq, warnings.toSeq)
}
