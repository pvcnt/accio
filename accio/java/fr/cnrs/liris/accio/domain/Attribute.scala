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

import fr.cnrs.liris.lumos.domain.{DataType, Value}

/**
 * Definition of an operator port (either input or output).
 *
 * @param name
 * @param dataType
 * @param help
 * @param defaultValue Default value taken by this input if none is specified. It should be empty for output ports.
 * @param optional
 * @param aspects
 */
case class Attribute(
  name: String,
  dataType: DataType,
  help: Option[String] = None,
  defaultValue: Option[Value] = None,
  optional: Boolean = false,
  aspects: Set[String] = Set.empty)
