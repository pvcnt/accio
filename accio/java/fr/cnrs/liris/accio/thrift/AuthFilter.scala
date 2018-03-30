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

package fr.cnrs.liris.accio.thrift

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.Service
import com.twitter.finatra.thrift.{ThriftFilter, ThriftRequest}
import com.twitter.util.Future
import fr.cnrs.liris.accio.api.UserInfo
import fr.cnrs.liris.accio.auth.AuthChain

@Singleton
final class AuthFilter @Inject()(chain: AuthChain) extends ThriftFilter {
  override def apply[T, Rep](request: ThriftRequest[T], service: Service[ThriftRequest[T], Rep]): Future[Rep] = {
    val credentials = request.clientId.map(_.name)
    chain.authenticate(credentials).flatMap {
      case None => Future.exception(WrongCredentialsException(credentials))
      case Some(userInfo) => UserInfo.let(userInfo)(service(request))
    }
  }
}