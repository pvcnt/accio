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

package fr.cnrs.liris.infra.httpserver

import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future

final class CorsFilter(domains: Set[String]) extends SimpleFilter[Request, Response] {
  private[this] val cors = {
    val allowsOrigin =
      if (domains.isEmpty) {
        { origin: String => Some(origin) }
      } else {
        { origin: String => Some(origin).filter(domains.contains) }
      }
    val allowsMethods = { method: String => Some(Seq("GET", "POST", "PUT", "DELETE")) }
    val allowsHeaders = { headers: Seq[String] => Some(headers) }

    val policy = Cors.Policy(allowsOrigin, allowsMethods, allowsHeaders)
    new Cors.HttpFilter(policy)
  }

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    cors.apply(request, service)
  }
}