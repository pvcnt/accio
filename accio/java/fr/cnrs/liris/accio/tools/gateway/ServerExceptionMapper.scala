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

package fr.cnrs.liris.accio.tools.gateway

import com.google.inject.Singleton
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import fr.cnrs.liris.accio.api.thrift.{ErrorCode, ServerException}

@Singleton
final class ServerExceptionMapper extends ExceptionMapper[ServerException] {
  override def toResponse(request: Request, e: ServerException): Response = {
    e.code match {
      case ErrorCode.Unauthenticated => Response(Status.Unauthorized)
      case ErrorCode.NotFound => Response(Status.NotFound)
      case ErrorCode.AlreadyExists => Response(Status.Conflict)
      case ErrorCode.FailedPrecondition => Response(Status.BadRequest)
      case ErrorCode.InvalidArgument => Response(Status.BadRequest)
      case ErrorCode.Unimplemented => Response(Status.NotImplemented)
      case _ => Response(Status.InternalServerError)
    }
  }
}
