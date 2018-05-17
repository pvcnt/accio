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

import com.google.inject.{Provides, Singleton}
import com.twitter.finagle.Thrift
import com.twitter.finagle.thrift.RichClientParam
import com.twitter.inject.TwitterModule
import com.twitter.util.Duration
import fr.cnrs.liris.infra.httpserver.CorsFilter
import fr.cnrs.liris.lumos.server.LumosService

object GatewayModule extends TwitterModule {
  private[this] val serverFlag = flag[String]("server", "Address of the Lumos server")
  private[this] val timeoutFlag = flag[Duration]("timeout", Duration.Top, "Timeout when issuing a request to the server")
  private[this] val corsDomainsFlag = flag("cors.domains", Seq.empty[String], "Domains for which to enable CORS support")

  @Singleton
  @Provides
  def providesClient: LumosService.MethodPerEndpoint = {
    val service = Thrift.client
      .withRequestTimeout(timeoutFlag())
      .withSessionQualifier.noFailFast
      .withSessionQualifier.noFailureAccrual
      .newService(serverFlag())
    val params = RichClientParam()
    LumosService.MethodPerEndpoint(LumosService.ServicePerEndpointBuilder.servicePerEndpoint(service, params))
  }

  @Singleton
  @Provides
  def providesCorsFilter: CorsFilter = new CorsFilter(corsDomainsFlag().toSet)
}