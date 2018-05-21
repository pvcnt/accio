/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the ter ms of the GNU General Public License as published by
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

import com.google.inject.Singleton
import com.twitter.finatra.thrift.exceptions.ExceptionMapper
import com.twitter.util.Future

@Singleton
final class ServerExceptionMapper extends ExceptionMapper[ServerException, ServerError] {
  override def handleException(e: ServerException): Future[ServerError] =
    e match {
      case ServerException.NotFound(resourceType, resourceName) =>
        Future.exception(ServerError(
          code = ErrorCode.NotFound,
          resourceType = Some(resourceType),
          resourceName = Some(resourceName)))
      case ServerException.AlreadyExists(resourceType, resourceName) =>
        Future.exception(ServerError(
          code = ErrorCode.AlreadyExists,
          resourceType = Some(resourceType),
          resourceName = Some(resourceName)))
      case ServerException.FailedPrecondition(resourceType, resourceName, errors) =>
        Future.exception(ServerError(
          code = ErrorCode.FailedPrecondition,
          resourceType = Some(resourceType),
          resourceName = Some(resourceName),
          errors = Some(errors.map(toThrift))))
      case ServerException.InvalidArgument(errors) =>
        Future.exception(ServerError(
          code = ErrorCode.InvalidArgument,
          errors = Some(errors.map(toThrift))))
      case ServerException.Unauthenticated => Future.exception(ServerError(ErrorCode.Unauthenticated))
    }

  private def toThrift(obj: ServerException.FieldViolation): FieldViolation = {
    FieldViolation(obj.message, obj.field)
  }
}