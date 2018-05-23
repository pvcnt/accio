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

package fr.cnrs.liris.infra.thriftserver

sealed abstract class ServerException(message: String) extends Exception(message)

object ServerException {

  case class AlreadyExists(resourceType: String, resourceName: String)
    extends ServerException(s"The $resourceType resource named $resourceName already exists")

  case class NotFound(resourceType: String, resourceName: String)
    extends ServerException(s"The $resourceType resource named $resourceName was not found")

  case class InvalidArgument(errors: Seq[FieldViolation])
    extends ServerException(s"Some request arguments were invalid:\n  ${errors.mkString("\n  ")}")

  case class FailedPrecondition(resourceType: String, resourceName: String, errors: Seq[FieldViolation])
    extends ServerException(s"Some preconditions failed on the $resourceType resource named $resourceName:\n  ${errors.mkString("\n  ")}")

  case object Unauthenticated
    extends ServerException("Provided credentials could not authenticate the client")

  case class FieldViolation(message: String, field: String)
}