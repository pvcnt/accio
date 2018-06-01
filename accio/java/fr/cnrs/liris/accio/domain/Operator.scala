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

package fr.cnrs.liris.accio.domain

import fr.cnrs.liris.lumos.domain.{DataType, RemoteFile, Value}

/**
 * Definition of an operator. An operator is the basic processing unit of Accio. It takes a typed
 * list of inputs and produces a typed list of outputs. The actual implementation of an operator is
 * provided by an executable binary, which follows a well-defined protocol.
 *
 * @param name        Operator name, unique among all operators.
 * @param category    A category used to organize operators. This is not interpreted by Accio and
 *                    only used for presentational purposes.
 * @param executable  Binary file providing the operator's implementation.
 * @param help        One-line help text.
 * @param description Longer description of what the operator does.
 * @param inputs      Definition of the inputs the operator consumes.
 * @param outputs     Definition of the outputs the operator produces.
 * @param deprecation Deprecation message, if this operator is actually deprecated. Deprecated
 *                    operators are actually usable, but a warning may be produced.
 * @param unstable    Whether this operator is unstable, will produce deterministic outputs given
 *                    the some inputs. Unstable operators need a random seed specified to produce
 *                    deterministic outputs.
 */
case class Operator(
  name: String,
  executable: RemoteFile,
  category: String = "misc",
  help: Option[String] = None,
  description: Option[String] = None,
  inputs: Seq[Attribute] = Seq.empty,
  outputs: Seq[Attribute] = Seq.empty,
  deprecation: Option[String] = None,
  unstable: Boolean = false)

/**
 * Definition of an operator attribute (either input or output).
 *
 * @param name         Attribute name, unique among all attributes (either input or output) of
 *                     the operator.
 * @param dataType     Data type this attribute accepts.
 * @param help         One-line help text.
 * @param defaultValue Default value taken by this input if none is specified. If provided, its
 *                     data type should be consistent with the attribute's data type. It should be
 *                     empty for output ports.
 * @param optional     Whether this attribute is optional. An optional attribute means that its
 *                     value may be left empty. However, an attribute with a default value is *not*
 *                     marked as optional (as its value will ultimately be filled with the default
 *                     value if not provided beforehand).
 * @param aspects      Aspects used to refine the data type of this attribute.
 */
case class Attribute(
  name: String,
  dataType: DataType,
  help: Option[String] = None,
  defaultValue: Option[Value] = None,
  optional: Boolean = false,
  aspects: Set[String] = Set.empty)
