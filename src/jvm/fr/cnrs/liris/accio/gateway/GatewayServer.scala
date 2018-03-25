/*
 * Accio is a program whose purpose is to study location privacy.
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

import java.io.InputStreamReader

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.util.StatusPrinter
import com.google.common.io.CharStreams
import com.twitter.finagle.Dtab
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

object GatewayServerMain extends GatewayServer

class GatewayServer extends HttpServer {
  private[this] val uiFlag = flag("ui", false, "Whether to enable the web-based user interface")

  loadLogbackConfig()
  readDtab()

  override protected def modules = Seq(GatewayModule)

  override protected def jacksonModule = AccioFinatraJacksonModule

  override protected def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[CorsFilter](beforeRouting = true)
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .add[HealthController]
      .add[ApiController]
    if (uiFlag()) {
      router.add[UiController]
    }
  }

  private def readDtab() = {
    val is = getClass.getClassLoader.getResourceAsStream(s"fr/cnrs/liris/accio/gateway/dtab.txt")
    if (null != is) {
      val content = CharStreams.readLines(new InputStreamReader(is)).asScala.mkString("")
      is.close()
      val dtab = Dtab.read(content)
      Dtab.setBase(dtab)
      logger.info(s"Read Dtab.base from resource: $dtab")
    }
  }

  private def loadLogbackConfig() = {
    val is = getClass.getClassLoader.getResourceAsStream(s"fr/cnrs/liris/accio/gateway/logback.xml")
    if (null != is) {
      // We assume SLF4J is bound to logback in the current environment.
      val ctx = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
      try {
        val configurator = new JoranConfigurator
        configurator.setContext(ctx)
        // Call context.reset() to clear any previous configuration, e.g. default
        // configuration. For multi-step configuration, omit calling context.reset().
        ctx.reset()
        configurator.doConfigure(is)
      } catch {
        case _: JoranException => // StatusPrinter will handle this.
      }
      StatusPrinter.printInCaseOfErrorsOrWarnings(ctx)
    }
  }
}