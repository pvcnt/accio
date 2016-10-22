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

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonSubTypes}

/**
 * An exploration is a way to explore a space of parameters.
 */
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[SingletonExploration], name = "value"),
  new JsonSubTypes.Type(value = classOf[ListExploration], name = "values"),
  new JsonSubTypes.Type(value = classOf[RangeExploration], name = "from")
))
@JsonIgnoreProperties(ignoreUnknown = true)
sealed trait Exploration

/**
 * An exploration which only explores only a single value. It is mainly useful to override the
 * value of a parameter.
 *
 * @param value New parameter's value.
 */
case class SingletonExploration(value: Any) extends Exploration

/**
 * An exploration which explores a fixed set of values.
 *
 * @param values List of values taken by the parameter.
 */
case class ListExploration(values: Set[Any]) extends Exploration

/**
 * An exploration which explores a range of values.
 *
 * @param from First value taken by the parameter.
 * @param to   Last value taken by the parameter.
 * @param step Step between two values of parameter.
 */
case class RangeExploration(from: Any, to: Any, step: Any, log: Boolean = false, log2: Boolean = false, log10: Boolean = false) extends Exploration