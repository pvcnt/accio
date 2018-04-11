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

package fr.cnrs.liris.accio.api

import fr.cnrs.liris.accio.api.thrift.{ErrorCode, ErrorDetails, FieldViolation, ServerException}

object Errors {
  def notFound(resourceType: String, resourceName: String): ServerException = {
    ServerException(
      code = ErrorCode.NotFound,
      message = Some(s"The $resourceType was not found: $resourceName"),
      details = Some(ErrorDetails(resourceType = Some(resourceType), resourceName = Some(resourceName))))
  }

  def alreadyExists(resourceType: String, resourceName: String): ServerException = {
    ServerException(
      code = ErrorCode.AlreadyExists,
      message = Some(s"The $resourceType already exists: $resourceName"),
      details = Some(ErrorDetails(resourceType = Some(resourceType), resourceName = Some(resourceName))))
  }

  def unauthenticated: ServerException = ServerException(code = ErrorCode.Unauthenticated)

  def failedPrecondition(resourceType: String, resourceName: String, message: String): ServerException = {
    ServerException(
      code = ErrorCode.FailedPrecondition,
      message = Some(message),
      details = Some(ErrorDetails(resourceType = Some(resourceType), resourceName = Some(resourceName))))
  }

  def badRequest(resourceType: String, errors: Seq[FieldViolation], warnings: Seq[FieldViolation]): ServerException = {
    ServerException(
      code = ErrorCode.InvalidArgument,
      message = Some(s"The provided $resourceType is invalid"),
      details = Some(ErrorDetails(resourceType = Some(resourceType), errors = errors, warnings = warnings)))
  }
}
