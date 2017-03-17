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

/**
 * Traits to force the initialisation of Logback's configuration, following Pants conventions. It should be included
 * only once per application, typically in the class implementing the `main()` method.
 *
 * By default, logback.xml is only loaded if present at the root of resources path. However, because Pants puts
 * resources in namespaces. This trait loads a logback.xml file located under the same package than this class
 * (i.e., the concrete class implementing this trait).
 */
trait LogbackConfigurator extends StrictLogging {
  {
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
}