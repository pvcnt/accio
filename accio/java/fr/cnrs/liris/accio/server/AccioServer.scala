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

package fr.cnrs.liris.accio.server

import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.filters._
import com.twitter.finatra.thrift.routing.ThriftRouter
import fr.cnrs.liris.accio.discovery.DiscoveryModule
import fr.cnrs.liris.accio.domain.DataTypes
import fr.cnrs.liris.accio.scheduler.install.SchedulerModule
import fr.cnrs.liris.accio.validation.ValidationModule
import fr.cnrs.liris.infra.thriftserver.{AuthFilter, AuthModule}
import fr.cnrs.liris.lumos.transport.EventTransportModule

object AccioServerMain extends AccioServer

class AccioServer extends ThriftServer {
  private[this] val executorUri = flag("executor_uri", "/var/lib/accio/executor.jar", "URI to the executor")

  init {
    DataTypes.register()
  }

  override protected def modules = Seq(
    AuthModule,
    DiscoveryModule,
    ValidationModule,
    SchedulerModule,
    EventTransportModule)

  override protected def configureThrift(router: ThriftRouter): Unit = {
    router
      .filter[LoggingMDCFilter]
      .filter[TraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter[AccessLoggingFilter]
      .filter[StatsFilter]
      .filter[AuthFilter]
      .filter[ExceptionMappingFilter]
      .add[AccioServiceController]
  }
}