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

import fr.cnrs.liris.lumos.domain.{AttrValue, ErrorDatum, MetricValue}

/**
 * Result of the operator. It is written by the operator's binary into a file specified as a
 * command-line argument.
 *
 * @param successful Whether the execution of the operator finished successfully.
 * @param artifacts  Arfifacts produced by the operator execution. This should be left empty in
 *                   case of a failure. If filled, there should be exactly one an artifact per
 *                   output port of the operator.
 * @param metrics    Metrics produced by the operator execution. This can always be filled, whether
 *                   or not the execution was successful.
 * @param error      Structured information about an exception that may have occurred. This can
 *                   only be filled in case of a failure.
 */
case class OpResult(
  successful: Boolean,
  artifacts: Seq[AttrValue] = Seq.empty,
  metrics: Seq[MetricValue] = Seq.empty,
  error: Option[ErrorDatum] = None)