/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

package fr.cnrs.liris.accio.validation

import scala.collection.mutable

case class ValidationResult(errors: Seq[ValidationResult.FieldViolation], warnings: Seq[ValidationResult.FieldViolation]) {
  def isValid: Boolean = errors.isEmpty

  def isInvalid: Boolean = errors.nonEmpty

  def +(other: ValidationResult): ValidationResult =
    ValidationResult(errors ++ other.errors, warnings ++ other.warnings)
}

object ValidationResult {

  case class FieldViolation(message: String, field: String)

  class Builder {
    private[this] val errors = mutable.ListBuffer.empty[FieldViolation]
    private[this] val warnings = mutable.ListBuffer.empty[FieldViolation]

    def error(violation: FieldViolation): Builder = {
      errors += violation
      this
    }

    def warn(violation: FieldViolation): Builder = {
      warnings += violation
      this
    }

    def build: ValidationResult = ValidationResult(errors.toList, warnings.toList)
  }

}