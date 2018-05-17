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

package fr.cnrs.liris.lumos.domain

sealed trait Status

object Status {

  case object Ok extends Status

  case class AlreadyExists(jobName: String) extends Status

  case class NotFound(jobName: String) extends Status

  case class InvalidArgument(errors: Seq[FieldViolation]) extends Status

  case class FailedPrecondition(jobName: String, errors: Seq[FieldViolation] = Seq.empty) extends Status

  case class FieldViolation(message: String, field: String)

}
