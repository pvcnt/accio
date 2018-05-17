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

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.Service
import com.twitter.finagle.thrift.ClientId
import com.twitter.finatra.thrift.{ThriftFilter, ThriftRequest}
import com.twitter.util.Future

@Singleton
final class AuthFilter @Inject()(chain: AuthChain) extends ThriftFilter {
  override def apply[T, Rep](request: ThriftRequest[T], service: Service[ThriftRequest[T], Rep]): Future[Rep] = {
    if (request.clientId.contains(AuthFilter.MasterClientId)) {
      UserInfo.let(AuthFilter.MasterUserInfo)(service(request))
    } else {
      val credentials = request.clientId.map(_.name)
      chain.authenticate(credentials).flatMap {
        case None => Future.exception(ServerException.Unauthenticated)
        case Some(userInfo) => UserInfo.let(userInfo)(service(request))
      }
    }
  }
}

object AuthFilter {
  val MasterClientId = ClientId("master:" + UUID.randomUUID().toString.replace("-", ""))
  val MasterUserInfo = UserInfo("system:master", None, Set("system:master", "system:authenticated"))
}