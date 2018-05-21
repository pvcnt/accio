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

package fr.cnrs.liris.lumos.gateway

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import fr.cnrs.liris.lumos.server.{ErrorCode, ServerError}

@Singleton
final class ServerErrorMapper @Inject()(response: ResponseBuilder) extends ExceptionMapper[ServerError] {
  override def toResponse(request: Request, e: ServerError): Response = {
    e.code match {
      case ErrorCode.Unauthenticated => response.unauthorized(e)
      case ErrorCode.NotFound => response.notFound(e)
      case ErrorCode.AlreadyExists => response.conflict(e)
      case ErrorCode.FailedPrecondition => response.badRequest(e)
      case ErrorCode.InvalidArgument => response.badRequest(e)
      case ErrorCode.Unimplemented => response.notImplemented
      case _ => response.internalServerException(e)
    }
  }
}
