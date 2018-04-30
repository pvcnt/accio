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

package fr.cnrs.liris.accio.agent

import com.google.inject.Singleton
import com.twitter.finatra.thrift.exceptions.ExceptionMapper
import com.twitter.util.Future
import fr.cnrs.liris.accio.api.Errors
import fr.cnrs.liris.accio.api.thrift.ServerException
import fr.cnrs.liris.finatra.auth.UnauthenticatedException

@Singleton
final class AgentExceptionMapper extends ExceptionMapper[UnauthenticatedException, ServerException] {
  override def handleException(e: UnauthenticatedException): Future[ServerException] = {
    Future.exception(Errors.unauthenticated)
  }
}
