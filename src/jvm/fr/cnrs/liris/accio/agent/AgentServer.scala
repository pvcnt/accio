/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.agent

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.util.StatusPrinter
import com.google.inject.Module
import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.filters._
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.util.FuturePool
import fr.cnrs.liris.accio.core.dsl.inject.DslModule
import fr.cnrs.liris.accio.core.filesystem.inject.FileSystemModule
import fr.cnrs.liris.accio.core.scheduler.inject.SchedulerModule
import fr.cnrs.liris.accio.core.statemgr.inject.StateManagerModule
import fr.cnrs.liris.accio.core.storage.inject.StorageModule
import fr.cnrs.liris.privamov.ops.OpsModule
import org.slf4j.LoggerFactory

object AgentServerMain extends AgentServer

class AgentServer extends ThriftServer {
  loadLogbackConfig()

  override protected def modules: Seq[Module] = Seq(
    FileSystemModule,
    StateManagerModule,
    SchedulerModule,
    StorageModule,
    DslModule,
    AgentModule,
    OpsModule)

  override protected def configureThrift(router: ThriftRouter): Unit = {
    router
      .filter[LoggingMDCFilter]
      .filter[TraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter[AccessLoggingFilter]
      .filter[StatsFilter]
      .add[AgentController]
  }

  override protected def start(): Unit = {
    val observer = injector.instance[LostTaskObserver]
    FuturePool.unboundedPool(observer.run())
    onExit {
      observer.kill()
    }
  }

  private def loadLogbackConfig() = {
    val is = getClass.getClassLoader.getResourceAsStream(s"fr/cnrs/liris/accio/agent/logback.xml")
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