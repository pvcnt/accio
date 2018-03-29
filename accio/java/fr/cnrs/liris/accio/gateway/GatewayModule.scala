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

package fr.cnrs.liris.accio.gateway

import com.google.inject.{Provides, Singleton}
import com.twitter.finagle.Thrift
import com.twitter.finagle.thrift.RichClientParam
import com.twitter.inject.TwitterModule
import com.twitter.util.Duration
import fr.cnrs.liris.accio.agent.{AgentService, AgentService$FinagleClient}

object GatewayModule extends TwitterModule {
  private[this] val serverFlag = flag[String]("agent.server", "Address of the Accio agent")
  private[this] val timeoutFlag = flag[Duration]("agent.timeout", Duration.Top, "Timeout when issuing a request to the agent")

  @Singleton
  @Provides
  def providesClient: AgentService$FinagleClient = {
    val service = Thrift.client
      .withRequestTimeout(timeoutFlag())
      .newService(serverFlag())
    val params = RichClientParam()
    new AgentService.FinagledClient(service, params)
  }
}