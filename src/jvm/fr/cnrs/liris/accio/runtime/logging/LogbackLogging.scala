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

package fr.cnrs.liris.accio.runtime.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.util.StatusPrinter
import com.typesafe.scalalogging.StrictLogging
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler

trait LogbackLogging extends StrictLogging {
  loadLogbackConfig()
  attemptSlf4jBridgeHandlerInstallation()

  private def loadLogbackConfig() = {
    val logbackPath = getClass.getPackage.getName.replace(".", "/") + "/logback.xml"
    val is = getClass.getClassLoader.getResourceAsStream(logbackPath)
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
      logger.debug(s"Loaded logback configuration from resource $logbackPath")
    }
  }

  private def attemptSlf4jBridgeHandlerInstallation(): Unit = {
    if (!SLF4JBridgeHandler.isInstalled && canInstallBridgeHandler) {
      SLF4JBridgeHandler.removeHandlersForRootLogger()
      SLF4JBridgeHandler.install()
      logger.info("org.slf4j.bridge.SLF4JBridgeHandler installed")
    }
  }

  private def canInstallBridgeHandler: Boolean = {
    // We do not want to attempt to install the bridge handler if the JDK14LoggerFactory
    // exists on the classpath. See: http://www.slf4j.org/legacy.html#jul-to-slf4j
    try {
      Class.forName("org.slf4j.impl.JDK14LoggerFactory", false, this.getClass.getClassLoader)
      logger.warn("Detected [org.slf4j.impl.JDK14LoggerFactory] on classpath. SLF4JBridgeHandler cannot be installed, see: http://www.slf4j.org/legacy.html#jul-to-slf4j")
      false
    } catch {
      case _: ClassNotFoundException => true
    }
  }
}