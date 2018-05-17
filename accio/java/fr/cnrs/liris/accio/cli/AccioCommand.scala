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

package fr.cnrs.liris.accio.cli

import com.twitter.finagle.Thrift
import com.twitter.finagle.thrift.{ClientId, RichClientParam}
import com.twitter.util.Duration
import fr.cnrs.liris.accio.server.AccioService
import fr.cnrs.liris.infra.cli.Command

trait AccioCommand extends Command {
  private[this] val serverFlag = flag[String]("server", "Server address")
  private[this] val timeoutFlag = flag("timeout", Duration.Top, "Server address")
  private[this] val credentialsFlag = flag[String]("credentials", "Credentials")

  protected lazy val client: AccioService.MethodPerEndpoint = {
    var builder = Thrift.client
      .withRequestTimeout(timeoutFlag())
      .withSessionQualifier.noFailFast
      .withSessionQualifier.noFailureAccrual
    credentialsFlag.get.foreach(credentials => builder = builder.withClientId(ClientId(credentials)))
    val service = builder.newService(serverFlag())
    val params = RichClientParam()
    AccioService.MethodPerEndpoint(AccioService.ServicePerEndpointBuilder.servicePerEndpoint(service, params))
  }
}
