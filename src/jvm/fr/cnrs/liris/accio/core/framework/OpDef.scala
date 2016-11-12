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

import com.fasterxml.jackson.annotation.JsonProperty
import fr.cnrs.liris.accio.core.api.Operator

/**
 * Definition of an operator. Actual implementation is done inside an [[Operator]] class.
 *
 * @param name        Operator name. Should be unique among all operators.
 * @param category    Category. Only used for presentational purposes.
 * @param help        One-line help text.
 * @param description Longer description of what the operator does.
 * @param inputs      Definition of inputs the operator consumes.
 * @param outputs     Definition of outputs the operator produces.
 * @param deprecation Deprecation message, if this operator is actually deprecated.
 */
case class OpDef(
  name: String,
  category: String,
  help: Option[String],
  description: Option[String],
  inputs: Seq[InputArgDef],
  outputs: Seq[OutputArgDef],
  deprecation: Option[String])

/**
 * Utils for [[OpDef]].
 */
object OpDef {
  /**
   * Pattern for valid operator names.
   */
  val NamePattern = "[A-Z][a-zA-Z0-9_]+"

  /**
   * Regex for valid operator names.
   */
  val NameRegex = ("^" + NamePattern + "$").r

  /**
   * Pattern for valid port names.
   */
  val PortPattern = "[a-z][a-zA-Z0-9_]+"

  /**
   * Regex for valid port names.
   */
  val PortRegex = ("^" + PortPattern + "$").r
}

/**
 * Definition of an operator input.
 *
 * @param name         Input name. Should be unique among all inputs of a given operator.
 * @param help         One-line help text.
 * @param kind         Data type.
 * @param isOptional   Whether this parameter is optional and does not have to be specified.
 * @param defaultValue Default value taken by this input if none is specified.
 */
case class InputArgDef(
  name: String,
  help: Option[String],
  @JsonProperty("type") kind: DataType,
  isOptional: Boolean,
  defaultValue: Option[Any])

/**
 * Definition of an operator output.
 *
 * @param name Output name. Should be unique among all outputs of a given operator.
 * @param help One-line help text.
 * @param kind Data type.
 */
case class OutputArgDef(name: String, help: Option[String], @JsonProperty("type") kind: DataType)