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

package fr.cnrs.liris.infra.logback

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.jul.LevelChangePropagator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, LoggerContext}
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.helpers.NOPAppender
import com.twitter.app.App
import com.twitter.util.logging.Slf4jBridgeUtility
import org.slf4j.{Logger, LoggerFactory}

trait LogbackConfigurator {
  this: App =>

  init {
    //initLogback()
    Slf4jBridgeUtility.attemptSlf4jBridgeHandlerInstallation()
  }

  protected final def initLogback(): Unit = {
    // We assume SLF4J is bound to logback in the current environment.
    val ctx = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    val rootLogger = ctx.getLogger(Logger.ROOT_LOGGER_NAME)
    ctx.reset()

    val patternEncoder = new PatternLayoutEncoder
    patternEncoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n")
    patternEncoder.setContext(ctx)
    patternEncoder.start()
    val consoleAppender = new ConsoleAppender[ILoggingEvent]
    consoleAppender.setName("Console")
    consoleAppender.setEncoder(patternEncoder)
    consoleAppender.setContext(ctx)
    consoleAppender.start()
    rootLogger.addAppender(consoleAppender)

    val levelChangePropagator = new LevelChangePropagator
    levelChangePropagator.setContext(ctx)
    levelChangePropagator.start()
    ctx.addListener(levelChangePropagator)
    rootLogger.setLevel(Level.INFO)
  }

  protected final def disableLogging(): Unit = {
    val ctx = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    ctx.reset()
    val rootLogger = ctx.getLogger(Logger.ROOT_LOGGER_NAME)
    val noopAppender = new NOPAppender[ILoggingEvent]
    noopAppender.setContext(ctx)
    noopAppender.start()
    rootLogger.addAppender(noopAppender)
  }
}