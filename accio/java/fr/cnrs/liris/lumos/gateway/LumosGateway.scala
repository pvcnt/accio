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

import com.google.inject.Module
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import fr.cnrs.liris.infra.httpserver.{CorsFilter, ScroogeFinatraJacksonModule}
import fr.cnrs.liris.infra.logback.LogbackConfigurator

object LumosGatewayMain extends LumosGateway

class LumosGateway extends HttpServer with LogbackConfigurator {
  private[this] val uiFlag = flag("ui", false, "Whether to enable the web-based user interface")
  private[this] val corsFlag = flag("cors", false, "Whether to enable CORS support")

  override protected def modules = Seq(GatewayModule)

  override protected def jacksonModule: Module = ScroogeFinatraJacksonModule

  override protected def configureHttp(router: HttpRouter): Unit = {
    if (corsFlag()) {
      router.filter[CorsFilter]
    }
    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .add[ApiController]

    if (uiFlag()) {
      router.add[UiController]
    }
  }

  override protected def warmup(): Unit = {
    handle[GatewayWarmupHandler]()
  }
}